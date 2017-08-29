package com.exo.blackstaridashboard.usb

import android.content.Context
import android.hardware.usb.*
import java.util.*
import java.util.logging.Logger


class AmpManager(val context: Context, val usbManager: UsbManager) {

    private val MANUFACTURER = "Blackstar"

    private val TIMEOUT = 0
    private val forceClaim = true
    private val permissionReceiver = PermissionReciever(this)
    private val detachReciever = DetachReceiver(this)


    private val log: Logger = Logger.getLogger("AmpManager")

    private var amp: UsbDevice? = null
    private lateinit var interruptInterface: UsbInterface
    private lateinit var outEndpoint: UsbEndpoint
    private lateinit var inEndpoint: UsbEndpoint

    private lateinit var connection: UsbDeviceConnection


    init {
        refresh();
    }

    fun refresh() {
        amp = usbManager.deviceList.values.find { it.manufacturerName.contains(MANUFACTURER) }
    }

    fun connect() {
        val permissionIntent = permissionReceiver.registerReceiver(context)
        usbManager.requestPermission(amp, permissionIntent);
    }

    public fun initializeConnection() {
        interruptInterface = amp!!.getInterface(0)
        val endpoints = IntRange(0, interruptInterface.endpointCount - 1)
                .map { interruptInterface.getEndpoint(it) }
        outEndpoint = endpoints.first { it.direction.equals(UsbConstants.USB_DIR_OUT) }
        inEndpoint = endpoints.first { it.direction.equals(UsbConstants.USB_DIR_IN) }
        connection = usbManager.openDevice(amp)
        connection.claimInterface(interruptInterface, forceClaim)


        val data = ByteArray(64)
        data[0] = 0x81.toByte()
        listOf(0x04, 0x03, 0x06, 0x02, 0x7a).map { it.toByte() }.zip(3..8)
                .forEach { (value, i) -> data[i] = value }

        transferOutData(data)
    }

    fun disconnect(device: UsbDevice) {
        if (device.equals(device)) {
            connection.close()
        }
    }

    private fun transferOutData(bytes: ByteArray): Int {
        return connection.bulkTransfer(outEndpoint, bytes, bytes.size, TIMEOUT) //do in another thread
    }

    fun transferInData() {
        val buffer = ByteArray(64)
        connection.bulkTransfer(inEndpoint, buffer, buffer.size, TIMEOUT) //do in another thread

        println(buffer)

        resolveDataPacket(buffer)
    }

    fun printAmpInfo(): String = amp.toString()

    private fun resolveDataPacket(packet: ByteArray): Map<String, Any> {
        return Header.findHandler(packet)(packet)
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
//            log.debug('Data from amp:: preset {0} has name: {1}\n'.format(preset, name))
            println("Data from amp:: preset {0} has name: {1}\n".format(preset, name))
            mapOf<kotlin.String, kotlin.Any>("presetName" to name, "preset" to preset)
        }),

        PRESET_CHANGED(0x02, byte1 = 0x06, handler = { packet ->
            println("Data from amp:: preset: {0}\n".format(packet[2]))
            mapOf("preset" to packet[2])
        }),

        PRESET_SETTINGS(0x02, byte1 = 0x05, handler = { packet ->
            val settings = AmpPreset.fromPacket(packet)
            println("Data from amp:: preset: {0} settings\n".format(packet[2]))
            mapOf("presetSettings" to settings)
        }),


        CONTROL_CHANGE(0x03, byte3 = 0x01, handler = { packet ->
            val id = packet[1]
            val control = com.exo.blackstaridashboard.usb.AmpManager.Control.findByCode(id)
            val value = packet[4]
            println("Data from amp:: control: {0} value: {1}".format(control, value))
            if (control == Control.DELAY_TIME)
                mapOf("delayTimeFine" to value)
            else
                mapOf("control" to value)
        }),

        CONTROL_CHANGE_FX(0x03, byte3 = 0x02, handler = { packet ->
            val id = packet[1]
            val control = Control.findByCode(id)
            when (control) {
                Control.DELAY_TIME -> {
                    val value = packet[4] + 256 * packet[5]
                    println("Data from amp:: control: {0} value: {1}".format(control, value))
                    mapOf<String, Any>("control" to value)
                }
                Control.DELAY_TYPE -> {
                    val delay_type = packet[4]
                    val delay_feedback = packet[5]
                    println("Data from amp:: delay_type: {0} delay_feedback: {1}\n"
                            .format(delay_type, delay_feedback))
                    mapOf("delay_type" to packet[4], "delay_feedback" to packet[5])
                }
                Control.REVERB_TYPE -> {
                    val reverb_type = packet[4]
                    val reverb_size = packet[5]
                    println("Data from amp:: reverb_type: {0} reverb_size: {1}\n".format(
                            reverb_type, reverb_size))
                    mapOf("reverb_type" to packet[4], "reverb_size" to packet[5])
                }
                Control.MOD_TYPE -> {
                    val mod_type = packet[4]
                    val mod_segval = packet[5]
                    println("Data from amp:: mod_type: {0} mod_segval: {1}\n".format(mod_type, mod_segval))
                    mapOf("mod_type" to packet[4], "mod_segval" to packet[5])
                }
                else -> {
                    println("Data from amp:: UNKNOWN FX: \n")
                    mapOf()
                }

            }
        }),

        CONTROL_CHANGE_ALL(0x03, byte3 = 0x2a, handler = { packet ->
            println("All controls info packet received\n'")
            Control.values()
                    .filter { it != Control.DELAY_TIME_COARSE }
                    .zip(0..Control.values().size - 1)
                    .map<Pair<Control, Int>, Pair<String, Any>> { (control, index) ->
                        if (control == Control.DELAY_TIME) {
                            kotlin.io.println(control.name + packet[index + 3].toInt())
                            control.name to ((packet[index + 4] * 256) + packet[index + 3])
                        } else {
                            kotlin.io.println(control.name + packet[index + 3].toInt())
                            control.name to packet[index + 3]
                        }
                    }
                    .toMap()
        }),

        STARTUP(0x07, handler = { packet ->
            println("Unhandled startup packet received\n'")
            mapOf()
        }),

        MODE_MANUAL(0x08, byte1 = 0x03, handler = { packet ->
            println("Data from amp:: manual mode: {0}".format(packet[4]))
            mapOf("manual_mode" to packet[4])
        }),

        MODE_TUNER(0x08, byte1 = 0x11, handler = { packet ->
            println("Data from amp:: tuner mode: {0}".format(packet[4]))
            mapOf("tuner_mode" to packet[4])
        }),


        //TODO: support in the future
        TUNER(0x09, handler = { packet -> mapOf() });

        companion object {
            fun findHandler(packet: ByteArray): (ByteArray) -> Map<String, Any> =
                    Header.values().asIterable()
                            .filter { it.byte0?.equals(packet[0]) ?: true }
                            .filter { it.byte1?.equals(packet[1]) ?: true }
                            .filter { it.byte2?.equals(packet[2]) ?: true }
                            .filter { it.byte3?.equals(packet[3]) ?: true }
                            .firstOrNull()?.handler ?: { mapOf() }
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

    class AmpPreset() {

        companion object {
            fun fromPacket(packet: ByteArray): AmpPreset {
                return AmpPreset()
            }
        }
    }
}
