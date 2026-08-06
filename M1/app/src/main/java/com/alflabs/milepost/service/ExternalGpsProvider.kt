package com.alflabs.milepost.service

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.location.Location
import android.os.Build
import com.alflabs.utils.ILogger
import com.github.petr_s.nmea.GpsSatellite
import com.github.petr_s.nmea.NMEAHandler
import com.github.petr_s.nmea.NMEAParser
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.IOException

class ExternalGpsProvider @AssistedInject constructor(
    @Assisted private val context: Context,
    private val logger: ILogger,
) {
    @AssistedFactory
    interface Factory {
        fun create(context: Context): ExternalGpsProvider
    }

    companion object {
        private val TAG = ExternalGpsProvider::class.java.simpleName
        private const val ACTION_USB_PERMISSION = "com.alflabs.milepost.USB_PERMISSION"
        private const val BAUD_RATE = 4800 // standard NMEA baud rate
        private const val VENDOR_ID = 1659 // Prolific
        private const val PRODUCT_ID = 8963 // BU-353N5
    }

    private var usbSerialPort: UsbSerialPort? = null
    private var usbIoManager: SerialInputOutputManager? = null
    private var isRunning = false
    private var locationCallback: LocationCallback? = null
    private val lock = Any()

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (ACTION_USB_PERMISSION == intent.action) {
                synchronized(lock) {
                    val device: UsbDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    }
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        device?.let {
                            logger.d(TAG, "USB Permission granted for device: ${it.deviceName}")
                        }
                    } else {
                        logger.d(TAG, "USB Permission denied for device: ${device?.deviceName}")
                    }
                }
            }
        }
    }

    suspend fun start(locationCallback: LocationCallback): Boolean {
        this.locationCallback = locationCallback
        isRunning = true

        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        val device = findUsbGpsDevice(usbManager) ?: return false

        logger.d(TAG, "Found USB GPS device: ${device.deviceName}")

        if (!usbManager.hasPermission(device)) {
            logger.d(TAG, "Requesting USB permission...")
            requestUsbPermission(usbManager, device)
            // Wait for permission (simplified: polling for now)
            var retry = 0
            while (!usbManager.hasPermission(device) && retry < 10 && isRunning) {
                delay(1000)
                retry++
            }
            if (!usbManager.hasPermission(device)) {
                logger.d(TAG, "USB permission not granted after timeout")
                return false
            }
        }

        connectAndRead(usbManager, device)
        return true
    }

    private fun findUsbGpsDevice(usbManager: UsbManager): UsbDevice? {
        return usbManager.deviceList.values.firstOrNull {
            it.vendorId == VENDOR_ID && it.productId == PRODUCT_ID
        }
    }

    private fun requestUsbPermission(usbManager: UsbManager, device: UsbDevice) {
        val intent = Intent(ACTION_USB_PERMISSION).apply {
            setPackage(context.packageName)
        }
        val permissionIntent = PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_MUTABLE
        )
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(usbReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(usbReceiver, filter)
        }
        usbManager.requestPermission(device, permissionIntent)
    }

    private suspend fun connectAndRead(usbManager: UsbManager, device: UsbDevice) = withContext(Dispatchers.IO) {
        val driver = UsbSerialProber.getDefaultProber().probeDevice(device)
        if (driver == null) {
            logger.d(TAG, "No driver found for device")
            return@withContext
        }

        val connection = usbManager.openDevice(driver.device)
        if (connection == null) {
            logger.d(TAG, "Could not open USB connection")
            return@withContext
        }

        val port = driver.ports[0]
        try {
            port.open(connection)
            port.setParameters(BAUD_RATE, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
            usbSerialPort = port

            val nmeaParser = NMEAParser(object : NMEAHandler {
                override fun onStart() {}
                override fun onFinish() {}
                override fun onLocation(location: Location) {
                    location.provider = "ExternalGps"
                    locationCallback?.onLocationResult(LocationResult.create(listOf(location)))
                }
                override fun onSatellites(satellites: MutableList<GpsSatellite>?) {}
                override fun onUnrecognized(sentence: String?) {}
                override fun onBadChecksum(expected: Int, actual: Int) {
                    logger.d(TAG, "NMEA Bad checksum: expected $expected, actual $actual")
                }
                override fun onException(e: Exception?) {
                    logger.d(TAG, "NMEA Parse exception: ${e?.message}")
                }
            })

            usbIoManager = SerialInputOutputManager(port, object : SerialInputOutputManager.Listener {
                private val lineBuffer = StringBuilder()

                override fun onNewData(data: ByteArray) {
                    val str = String(data)
                    for (char in str) {
                        if (char == '\n' || char == '\r') {
                            if (lineBuffer.isNotEmpty()) {
                                val line = lineBuffer.toString()
                                lineBuffer.setLength(0)
                                try {
                                    nmeaParser.parse(line)
                                } catch (e: Exception) {
                                    // logger.d(TAG, "NMEA Parse error: ${e.message}")
                                }
                            }
                        } else {
                            lineBuffer.append(char)
                        }
                    }
                }

                override fun onRunError(e: Exception) {
                    logger.d(TAG, "USB IO Error: ${e.message}")
                }
            })
            usbIoManager?.start()
            logger.d(TAG, "USB GPS connected and reading...")

        } catch (e: IOException) {
            logger.d(TAG, "Error opening USB port: ${e.message}")
        }
    }

    fun onDestroy() {
        isRunning = false
        try {
            context.unregisterReceiver(usbReceiver)
        } catch (e: Exception) {
            // ignore
        }
        usbIoManager?.stop()
        usbIoManager = null
        try {
            usbSerialPort?.close()
        } catch (e: IOException) {
            // ignore
        }
        usbSerialPort = null
        locationCallback = null
    }
}
