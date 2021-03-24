package com.example.simplebluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import splitties.toast.toast
import java.io.IOException
import java.util.*

class BTControl : AppCompatActivity() {

    private val TAG = "BTControl Activity"

    private lateinit var address : String
    private var isConnected = false
    private var ledIsOn = false

    private var mSocket: BluetoothSocket? = null
    private val mBluetooth: BluetoothAdapter by lazy { BluetoothAdapter.getDefaultAdapter() }

    val mUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private val progress: ProgressBar by lazy{ findViewById(R.id.progress) }
    private val txtConnection: TextView by lazy{ findViewById(R.id.textview_connected_to) }
    private val btnLed: Button by lazy{ findViewById(R.id.button_led_status) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_b_t_control)

        val newint = intent
        address = newint.getStringExtra(MainActivity.EXTRA_ADDRESS).toString()

        toast(address)

        ConnectBT().execute()

        btnLed.setOnClickListener {
            ledIsOn = !ledIsOn
            if (ledIsOn) {
                sendBTMessage("H")
                btnLed.text = getString(R.string.led_off)
            } else {
                sendBTMessage("L")
                btnLed.text = getString(R.string.led_on)
            }
        }
    }

    private fun sendBTMessage(send: String) {
        try {
            mSocket!!.outputStream.write(send.toByteArray())
        } catch (e: IOException) {
            e.printStackTrace()
            toast(e.localizedMessage!!)
        }
    }

    private inner class ConnectBT : AsyncTask<Void, Void, Void>() {
        private var connectSuccess = true

        override fun onPreExecute() {
            toast("Verbinde...")
            progress.visibility = View.VISIBLE
        }

        override fun doInBackground(vararg devices: Void?): Void? {
            try {
                if (mSocket == null || !isConnected) {
                    val device = mBluetooth.getRemoteDevice(address)
                    mSocket = device.createInsecureRfcommSocketToServiceRecord(mUUID)
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery()
                    mSocket!!.connect()
                }
            } catch (e: IOException) {
                Log.d(TAG, "Unable to connect")
                e.printStackTrace()
                connectSuccess = false
            }
            return null
        }

        override fun onPostExecute(result: Void?) {
            super.onPostExecute(result)

            if (!connectSuccess) {
                toast(getString(R.string.connect_failed))
                finish()
            } else {
                toast(getString(R.string.connect_success))
                isConnected = true
                txtConnection.text = getString(R.string.connected_to, address)
            }
            progress.visibility = View.GONE
        }
    }
}