package com.alflabs.milepostlib.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.location.Location
import androidx.annotation.VisibleForTesting
import com.alflabs.milepostlib.app.AppSettings
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
        const val ACTION_USB_PERMISSION = "com.alflabs.milepostlib.USB_PERMISSION"
        private const val BAUD_RATE = 4800 // standard NMEA baud rate

        // There's a long list of USB serial ids here:
        // https://github.com/mik3y/usb-serial-for-android/blob/master/usbSerialExamples/src/main/res/xml/device_filter.xml
        // We can also filter on manufacturer name or product name.
        // Specs: our hardware is a "GlobalSat BU-353N5 USB GNSS Receiver" w/ a Prolific PL2303GC
        // so that's going to be our default USB filter string.
        const val GLOBAL_SAT_BU353N5 = "1659:9123 Prolific USB-Serial GlobalSat BU-353N5"
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
            val filter = appSettings.usbGpsFilter().trim().ifBlank { GLOBAL_SAT_BU353N5 }
            device = findUsbGpsDevice(filter, usbManagerOpt.get())
            logger.d(TAG, "@@ USB GPS Device: $device")
        } else {
            device = null
        }
    }

    suspend fun start(locationCallback: LocationCallback): Boolean {
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

    private fun findUsbGpsDevice(filter: String, usbManager: UsbManager): UsbDevice? {
        val deviceList = usbManager.deviceList
        logger.d(TAG, "@@ USB Devices: $deviceList")
        return filterUsbDevices(filter, deviceList.values)
    }

    /**
     * Selects a USB device.
     * "filterStr" is a space-separated of parameters to match:
     * - DDDD:DDDD or HHHH:HHHH = VendorID:ProductID pairs (decimal or hex, 4 digits each).
     *   If multiple VID:PID are provided, accept the first one that matches.
     * - Any word without : is a case-insensitive keyword to match against ManufacturerName
     *   or ProductName, in any order.
     * - All filter fields must match.
     */
    @VisibleForTesting
    internal fun filterUsbDevices(
        filterStr: String,
        deviceList: Collection<UsbDevice?>): UsbDevice? {
        val parts = filterStr.split("\\s+".toRegex()).filter { it.isNotBlank() }
        val idFilters = mutableListOf<Pair<Int, Int>>()
        val keywords = mutableListOf<String>()

        for (part in parts) {
            if (part.contains(":")) {
                val subParts = part.split(":")
                if (subParts.size == 2) {
                    val vid = parseUsbId(part, subParts[0])
                    val pid = parseUsbId(part, subParts[1])
                    if (vid != null && pid != null) {
                        idFilters.add(vid to pid)
                    }
                }
            } else {
                keywords.add(part.lowercase())
            }
        }

        return deviceList.firstOrNull { device ->
            if (device == null) return@firstOrNull false

            // Check VID:PID filters: if any provided, at least one must match.
            val idMatch = if (idFilters.isEmpty()) {
                true
            } else {
                idFilters.any { (vid, pid) ->
                    device.vendorId == vid && device.productId == pid
                }
            }

            // Check keywords: all must match against ManufacturerName or ProductName.
            val keywordMatch = keywords.all { keyword ->
                (device.manufacturerName?.contains(keyword, ignoreCase = true) == true) ||
                        (device.productName?.contains(keyword, ignoreCase = true) == true)
            }

            idMatch && keywordMatch
        }
    }

    private fun parseUsbId(full: String, sid: String): Int? {
        // Full string is either "DDDD:DDDD", "HHHH:HHHH", or "0xHHHH:0xHHHH".
        // If we find 0x or A-F, we assume it's all hexadecimal (for both ids).
        // Otherwise, we assume both ids are in decimal.
        return try {
            when {
                full.contains("0x", ignoreCase = true) -> sid.replace("0x", "").toInt(16)
                full.any { it.uppercaseChar() in 'A'..'F' } -> sid.toInt(16)
                else -> sid.toInt(10)
            }
        } catch (_: NumberFormatException) {
            null
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
