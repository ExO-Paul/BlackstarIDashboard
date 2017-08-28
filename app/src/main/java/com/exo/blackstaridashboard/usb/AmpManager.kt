package com.exo.blackstaridashboard.usb

import android.content.Context
import android.hardware.usb.*


class AmpManager(val context: Context, val usbManager: UsbManager) {

    private val MANUFACTURER = "Blackstar"

    private val TIMEOUT = 0
    private val forceClaim = true
    private val permissionReceiver = PermissionReciever()

    private lateinit var amp: UsbDevice
    private lateinit var interruptInterface: UsbInterface
    private lateinit var outEndpoint: UsbEndpoint
    private lateinit var inEndpoint: UsbEndpoint

    private lateinit var connection: UsbDeviceConnection


    init {
        refresh();
    }

    fun refresh() {
        amp = usbManager.deviceList.values.first { it.manufacturerName.contains(MANUFACTURER) }
    }

    fun connect() {
        val permissionIntent = permissionReceiver.registerReceiver(context)
        usbManager.requestPermission(amp, permissionIntent);

        interruptInterface = amp.getInterface(0)
        val endpoints = IntRange(0, interruptInterface.endpointCount)
                .map { interruptInterface.getEndpoint(it) }
        outEndpoint = endpoints.first { it.direction.equals(UsbConstants.USB_DIR_OUT) }
        inEndpoint = endpoints.first { it.direction.equals(UsbConstants.USB_DIR_IN) }
        connection = usbManager.openDevice(amp)
        connection.claimInterface(interruptInterface, forceClaim)
    }

    fun disconnect(device: UsbDevice) {
        if (device.equals(device)) {
            connection.close()
        }
    }

    private fun transferOutData(bytes: ByteArray): Int {
        return connection.bulkTransfer(outEndpoint, bytes, bytes.size, TIMEOUT) //do in another thread
    }

    private fun transferInData() {
        val buffer = ByteArray(64)
        connection.bulkTransfer(inEndpoint, buffer, buffer.size, TIMEOUT) //do in another thread

        resolveDataPacket(buffer)

    }

    fun printAmpInfo(): String = amp.toString()

    private fun resolveDataPacket(packet: ByteArray) {
        val result = Header.findHandler(packet)(packet)

    }

    enum class Header(val byte0: Byte?,
                      val byte1: Byte? = null,
                      val byte2: Byte? = null,
                      val byte3: Byte? = null,
                      val handler: (ByteArray) -> Map<String, Any>) {
        PRESET_NAME(0x02, byte1 = 0x04, handler = { packet: ByteArray ->
            val preset = packet[2]
            val nameL = packet.slice(4..25).filter { it > 0 }
            val name = nameL.map { it.toChar() }.toString()
//                log.debug('Data from amp:: preset {0} has name: {1}\n'.format(preset, name))
            mapOf<kotlin.String, kotlin.Any>("presetName" to name, "preset" to preset)
        }),
        PRESET_CHANGED(0x02, byte1 = 0x05, handler = { bytes -> mapOf() }),
        PRESET_SETTINGS(0x02, byte1 = 0x06, handler = { bytes -> mapOf() }),

        CONTROL(0x03, handler = { bytes -> mapOf() }), //needs further packet investigation
        STARTUP(0x07, handler = { bytes -> mapOf() }),
        MODE_MANUAL(0x08, byte1 = 0x03, handler = { bytes -> mapOf() }),
        MODE_TUNER(0x08, byte1 = 0x11, handler = { bytes -> mapOf() }),

        TUNER(0x09, handler = { bytes -> mapOf() });

        companion object {
            fun findHandler(packet: ByteArray): (ByteArray) -> Map<String, Any>  =
                Header.values().asIterable()
                        .filter { it.byte0?.equals(packet[0]) ?: true }
                        .filter { it.byte1?.equals(packet[1]) ?: true }
                        .filter { it.byte2?.equals(packet[2]) ?: true }
                        .filter { it.byte3?.equals(packet[3]) ?: true }
                        .first().handler
        }
    }

    enum class Control(val code: Byte, val range: IntRange) {
        VOICE(0x01, 0..5),
        GAIN(0x02, 0..127),
        VOLUME(0x03, 0..127),
        BASS(0x04, 0..127),
        MIDDLE(0x05, 0..127),
        TREBLE(0x06, 0..127),
        ISF(0x07, 0..127),
        TVP_VALVE(0x08, 0..5),
        RESONANCE(0x0b, 0..127),
        PRESENCE(0x0c, 0..127),
        MASTER_VOLUME(0x0d, 0..127),
        TVP_SWITCH(0x0e, 0..1),
        MOD_SWITCH(0x0f, 0..1),
        DELAY_SWITCH(0x10, 0..1),
        REVERB_SWITCH(0x11, 0..1),
        MOD_TYPE(0x12, 0..3),
        MOD_SEGVAL(0x13, 0..31),
        MOD_MANUAL(0x14, 0..127), // Flanger only
        MOD_LEVEL(0x15, 0..127),
        MOD_SPEED(0x16, 0..127),
        DELAY_TYPE(0x17, 0..3),
        DELAY_FEEDBACK(0x18, 0..31), // Segment value
        DELAY_LEVEL(0x1a, 0..127),
        DELAY_TIME(0x1b, 0..2000),
        DELAY_TIME_COARSE(0x1c, 0..7),
        REVERB_TYPE(0x1d, 0..3),
        REVERB_SIZE(0x1e, 0..31), // Segment value
        REVERB_LEVEL(0x20, 0..127),
        FX_FOCUS(0x24, 1..3);

        companion object {
            fun findByCode(code: Byte) = Control.values().toList().find { it.code == code }
        }
    }
}
