package com.exo.blackstaridashboard.usb

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.hardware.usb.UsbManager
import android.hardware.usb.UsbDevice
import android.content.Intent
import android.content.Context
import android.content.IntentFilter
import android.os.Parcelable
import android.widget.Toast


class DetachReceiver(val ampManager: AmpManager): BroadcastReceiver() {


    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action

        if (UsbManager.ACTION_USB_DEVICE_DETACHED == action) {
            val device = intent.getParcelableExtra<Parcelable>(UsbManager.EXTRA_DEVICE) as UsbDevice
            ampManager.disconnect(device)
            Toast.makeText(ampManager.context, "Disconnected", Toast.LENGTH_SHORT)

        }
    }


    fun registerReceiver(context : Context) {
        val filter = IntentFilter("SMTH")
        context.registerReceiver(this, filter)
    }


}