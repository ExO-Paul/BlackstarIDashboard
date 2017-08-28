package com.exo.blackstaridashboard

import android.content.Context
import android.hardware.usb.UsbManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.exo.blackstaridashboard.usb.AmpManager
import com.exo.blackstaridashboard.usb.DetachReceiver
import kotlinx.android.synthetic.main.activity_dashboard.*


class DashboardActivity : AppCompatActivity() {

    val ampManager = AmpManager(this, getSystemService(Context.USB_SERVICE) as UsbManager)
    val detachReciever = DetachReceiver(ampManager)

    override fun onCreate(savedInstanceState: Bundle?) {
           super.onCreate(savedInstanceState)
           setContentView(R.layout.activity_dashboard)


        fillDeviceInfo()

        getInfoButton.setOnClickListener { fillDeviceInfo() }
        connectButton.setOnClickListener { ampManager.connect() }

    }

    private fun fillDeviceInfo() {
        ampManager.refresh()
        textSpace.text = ampManager.printAmpInfo()
    }

    private fun connect() {
        ampManager.connect()
    }
}

