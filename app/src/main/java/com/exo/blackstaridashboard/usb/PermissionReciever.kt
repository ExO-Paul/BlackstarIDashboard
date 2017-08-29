package com.exo.blackstaridashboard.usb

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Parcelable
import android.util.Log

class PermissionReciever(val ampManager: AmpManager) : BroadcastReceiver() {

    //TODO: Change
    private val ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION"

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (ACTION_USB_PERMISSION == action) {
            synchronized(this) {
                val device = intent.getParcelableExtra<Parcelable>(UsbManager.EXTRA_DEVICE) as UsbDevice

                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    ampManager.initializeConnection()
                    println("something")
                } else {
                    Log.d(ContentValues.TAG, "Permission denied for device " + device)
                }
            }
        }
    }

    fun registerReceiver(context : Context): PendingIntent? {
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        context.registerReceiver(this, filter)
        return PendingIntent.getBroadcast(context, 0, Intent(ACTION_USB_PERMISSION), 0)
    }

}


