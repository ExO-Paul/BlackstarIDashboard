package com.exo.blackstaridashboard.usb

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager



class AmpManager(val context: Context, val manager: UsbManager) {

    private val MANUFACTURER = "Blackstar"

    private var bytes: ByteArray = kotlin.ByteArray(256)
    private val TIMEOUT = 0
    private val forceClaim = true
    private val permissionReceiver = PermissionReciever()


    private var amp: UsbDevice? = null

    init {
        refresh();
    }

    fun refresh() {
        amp = manager.deviceList.values.firstOrNull { it.manufacturerName.contains(MANUFACTURER) }


        val intf = amp?.getInterface(0)
        val endpoint = intf?.getEndpoint(0)
        val connection = manager.openDevice(amp)
        connection.claimInterface(intf, forceClaim)
        connection.bulkTransfer(endpoint, bytes, bytes.size, TIMEOUT) //do in another thread
    }

    fun connect() {
        val permissionIntent = permissionReceiver.registerReceiver(context)
        manager.requestPermission(amp, permissionIntent);
    }

    fun printAmpInfo(): String {
        return amp.toString()
    }

}
