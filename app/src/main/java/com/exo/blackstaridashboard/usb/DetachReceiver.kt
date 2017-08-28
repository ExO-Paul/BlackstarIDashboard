package com.exo.blackstaridashboard.usb

import android.hardware.usb.UsbManager
import android.hardware.usb.UsbDevice
import android.content.Intent
import android.content.Context
import android.os.Parcelable


class DetachReceiver(val ampManager: AmpManager) {

    fun onReceive(context: Context, intent: Intent) {
        val action = intent.action

        if (UsbManager.ACTION_USB_DEVICE_DETACHED == action) {
            val device = intent.getParcelableExtra<Parcelable>(UsbManager.EXTRA_DEVICE) as UsbDevice
                ampManager.disconnect(device)
        }
    }

}