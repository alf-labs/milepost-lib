package com.alflabs.milepost.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.location.Location
import com.alflabs.milepost.app.AppSettings
import com.alflabs.utils.ILogger
import com.github.petr_s.nmea.GpsSatellite
import com.github.petr_s.nmea.NMEAHandler
import com.github.petr_s.nmea.NMEAParser
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Optional
import javax.inject.Singleton
import kotlin.jvm.optionals.getOrNull


@Singleton
class ExternalGpsProvider @Inject constructor(
    private val logger: ILogger,
    private val appSettings: AppSettings,
    private val usbManagerOpt: Optional<UsbManager>,
) {
    companion object {
        private val TAG = ExternalGpsProvider::class.java.simpleName
        const val ACTION_USB_PERMISSION = "com.alflabs.milepost.USB_PERMISSION"
        private const val BAUD_RATE = 4800 // standard NMEA baud rate

        // TBD change this to a preference string e.g. "1659:9123" (or hex, etc).
        // There's a long list of USB serial ids here:
        // https://github.com/mik3y/usb-serial-for-android/blob/master/usbSerialExamples/src/main/res/xml/device_filter.xml
        // We can also filter on manufacturer name or product name.
        private const val VENDOR_ID = 1659 // Prolific
        //        private const val PRODUCT_ID = 8963 // BU-353N5
        private const val PRODUCT_ID = 9123 // PL2303GC (for the BU-353N5)
        private const val MANUFACTURER_NAME = "Prolific"
        private const val PRODUCT_NAME = "USB-Serial"
    }

    private var device: UsbDevice? = null
    private var usbSerialPort: UsbSerialPort? = null
    private var usbIoManager: SerialInputOutputManager? = null


    suspend fun isUsbPermissionNeeded() : Boolean {
        setupDevice()
        return device != null
    }

    fun isUsbPermissionGranted() : Boolean {
        val granted = device != null && usbManagerOpt.getOrNull()?.hasPermission(device) ?: false
        logger.d(TAG, "@@ isUsbPermissionGranted = $granted")
        return granted
    }

    private suspend fun setupDevice() {
        if (appSettings.useExternalGps() && usbManagerOpt.isPresent()) {
            device = findUsbGpsDevice(usbManagerOpt.get())
            logger.d(TAG, "@@ USB GPS Device: $device")
        } else {
            device = null
        }
    }

    suspend fun start(locationCallback: LocationCallback): Boolean {
        // TBD: The purpose of this method to connect to an external USB GPS,
        // periodically parse NMEA messages, and provide them to the location
        // callback till onDestroy is called.
        //
        // This is called from LocationService.
        // Only the LocationCallback.onLocationResult() method is used.
        // Ideally we'll reuse the GMS LocationResult class to pass data
        // (we only read the fields location.time, latitude, longitude, and accuracy)
        // but if that's not possible we can just make our own data class.
        //
        // The method should ideally return false if we enumerate usb devices
        // and find no usb serial gps receiver at all.
        // That way the caller can fallback on internal gps.
        // In the unlikely event there's more than one external gps, we'll just pick
        // up the first one.
        //
        // Suggested implementation is to run this in a looper so that we can
        // handle disconnections and keep reconnecting as needed, then request
        // messages at about 1 Hz.
        // However since the caller is a lifecycleScope.launch coroutine, we could
        // make this use any kotlin coroutine mechanism for the loop.
        //
        // Specs: our hardware is a "GlobalSat BU-353N5 USB GNSS Receiver"
        // connected via a USB hub to the android tablet. We'll need a
        // USB Serial Driver of some kind (library or implemented here).

        if (device == null) {
            setupDevice()
        }
        val usbManager = usbManagerOpt.getOrNull() ?: return false
        val usbDevice = device ?: return false
        if (!usbManager.hasPermission(usbDevice)) {
            logger.d(TAG, "@@ Missing USB Permission")
            return false
        }

        logger.d(TAG, "@@ BEFORE connectAndRead --------- ")
        connectAndRead(usbManager, usbDevice, locationCallback)
        logger.d(TAG, "@@ AFTER connectAndRead --------- ")
        return true
    }

    private fun findUsbGpsDevice(usbManager: UsbManager): UsbDevice? {
        val deviceList = usbManager.deviceList
        logger.d(TAG, "@@ USB Devices: $deviceList")
        return deviceList.values.firstOrNull {
            it.vendorId == VENDOR_ID
                    && it.productId == PRODUCT_ID
                    && it.manufacturerName?.contains(MANUFACTURER_NAME, ignoreCase = true) == true
                    && it.productName?.contains(PRODUCT_NAME, ignoreCase = true) == true
        }
    }

    suspend fun requestUsbPermission(context: Context) {
        if (device == null) {
            setupDevice()
        }
        val usbManager = usbManagerOpt.getOrNull() ?: return
        val usbDevice = device ?: return

        val intent = Intent(ACTION_USB_PERMISSION).apply {
            setPackage(context.packageName)
        }
        val permissionIntent = PendingIntent.getBroadcast(
            context,
            /*requestCode=*/0,
            intent,
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        usbManager.requestPermission(usbDevice, permissionIntent)
    }


    private suspend fun connectAndRead(
        usbManager: UsbManager,
        device: UsbDevice,
        locationCallback: LocationCallback
    ) = withContext(Dispatchers.IO) {
        val driver = UsbSerialProber.getDefaultProber().probeDevice(device)
        if (driver == null) {
            logger.d(TAG, "@@ No driver found for device")
            return@withContext
        }

        val connection = usbManager.openDevice(driver.device)
        if (connection == null) {
            logger.d(TAG, "@@ Could not open USB connection")
            return@withContext
        }

        val port = driver.ports[0]
        try {
            port.open(connection)
            port.setParameters(BAUD_RATE, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
            usbSerialPort = port
            logger.d(TAG, "@@ USB GPS Port open")

            val nmeaParser = NMEAParser(object : NMEAHandler {
                override fun onStart() {}
                override fun onFinish() {}
                override fun onLocation(location: Location) {
                    location.provider = "ExternalGps"
                    logger.d(TAG, "@@ NMEA Parse location: $location")
                    locationCallback.onLocationResult(LocationResult.create(listOf(location)))
                }

                override fun onSatellites(satellites: MutableList<GpsSatellite>?) {}
                override fun onUnrecognized(sentence: String?) {}
                override fun onBadChecksum(expected: Int, actual: Int) {
                    logger.d(TAG, "@@ NMEA Bad checksum: expected $expected, actual $actual")
                }

                override fun onException(e: Exception?) {
                    logger.d(TAG, "@@ NMEA Parse exception: ${e?.message}")
                }
            })

            usbIoManager =
                SerialInputOutputManager(port, object : SerialInputOutputManager.Listener {
                    private val lineBuffer = StringBuilder()

                    override fun onNewData(data: ByteArray) {
                        val str = String(data)
                        for (char in str) {
                            if (char == '\n' || char == '\r') {
                                if (lineBuffer.isNotEmpty()) {
                                    val line = lineBuffer.toString()
                                    lineBuffer.setLength(0)
                                    try {
                                        logger.d(TAG, "@@ NMEA Parse line: $line")
                                        nmeaParser.parse(line)
                                    } catch (e: Exception) {
                                         logger.d(TAG, "@@ NMEA Parse error: ${e.message}")
                                    }
                                }
                            } else {
                                lineBuffer.append(char)
                            }
                        }
                    }

                    override fun onRunError(e: Exception) {
                        logger.d(TAG, "@@ USB IO Error: ${e.message}")
                    }
                })
            usbIoManager?.start()
            logger.d(TAG, "@@ USB GPS connected and reading...")

        } catch (e: IOException) {
            logger.d(TAG, "@@ Error opening USB port: ${e.message}")
        }
    }

    fun onDestroy() {
        logger.d(TAG, "@@ USB GPS closing...")
        usbIoManager?.stop()
        usbIoManager = null
        try {
            usbSerialPort?.close()
        } catch (e: IOException) {
            // ignore
        }
        usbSerialPort = null
    }
}
