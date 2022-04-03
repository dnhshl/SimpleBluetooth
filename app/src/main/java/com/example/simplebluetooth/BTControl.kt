package com.example.simplebluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import org.json.JSONException
import org.json.JSONObject
import splitties.toast.toast
import java.io.IOException
import java.util.*
import org.json.JSONArray

class  BTControl : AppCompatActivity() {

    private val TAG = "BTControl Activity"

    private lateinit var address : String
    private lateinit var value : String
    private var isConnected = false
    private var ledIsOn = false
    private var ledFlashing = false
    private var receivingData = false

    private val mHandler: Handler by lazy { Handler() }
    private lateinit var mRunnable: Runnable
    private var mSocket: BluetoothSocket? = null
    private val mBluetooth: BluetoothAdapter by lazy { BluetoothAdapter.getDefaultAdapter() }

    val mUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private val progress: ProgressBar by lazy{ findViewById(R.id.progress) }
    private val txtConnection: TextView by lazy{ findViewById(R.id.textview_connected_to) }
    private val btnLed: Button by lazy{ findViewById(R.id.button_led_status) }
    private val btnData: Button by lazy{ findViewById(R.id.button_data) }
    private val btnBlinken: Button by lazy{ findViewById(R.id.btnBlinken) }
    private val tvData: TextView by lazy{ findViewById(R.id.textview_value_received) }
    private val tvArray: TextView by lazy{ findViewById(R.id.tvArray) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_b_t_control)

        val newint = intent
        address = newint.getStringExtra(MainActivity.EXTRA_ADDRESS).toString()

        ConnectBT().execute()

        btnBlinken.setOnClickListener {
            ledFlashing = !ledFlashing
            val obj = JSONObject()
            if(ledFlashing) {
                obj.put("LEDBlinken", true)
                btnBlinken.text = getString(R.string.flashing_off)
            }else{
                obj.put("LEDBlinken", false)
                btnBlinken.text = getString(R.string.flashing_on)
            }
            obj.put("LED", if (ledIsOn) "H" else "L")
            sendBTMessage("!" + obj.toString() + "?")
        }

        btnLed.setOnClickListener {
            ledIsOn = !ledIsOn
            val obj = JSONObject()
            if (ledIsOn) {
                obj.put("LED", "H")
                btnLed.text = getString(R.string.led_off)
            } else {
                obj.put("LED", "L")
                btnLed.text = getString(R.string.led_on)
            }
            obj.put("LEDBlinken", ledFlashing)
            sendBTMessage("!" + obj.toString() + "?")
        }

        btnData.setOnClickListener {
            receivingData = !receivingData
            if (receivingData) dataStart() else dataStop()
        }
    }

    private fun dataStart() {
        if (!isConnected) return
        if (!receivingData) return
        btnData.text = getString(R.string.data_stop)
        // starte Handler-Runnable zum Lesen des Buffers
        mRunnable = Runnable {
            mHandler.postDelayed(mRunnable, 1000)
            readBuffer()
        }
        mHandler.postDelayed(mRunnable, 1000)
    }

    private fun dataStop() {
        btnData.text = getString(R.string.data_start)
        if (mRunnable != null) mHandler.removeCallbacks(mRunnable)
        receivingData = false
    }


    private fun readBuffer() {
        try {
            val mInputStream = mSocket!!.inputStream
            val buffer = ByteArray(2048)
            val bytes = mInputStream.read(buffer)
            // buffer in String umwandeln
            val jsonStrings = String(buffer, 0, bytes)
            // an ! trennen
            val singleJsonStrings = jsonStrings.split("!")
            // jeden einzelnen getrennten String betrachten
            singleJsonStrings.forEach { jsonstring ->
                // Endet der String mit ?
                if (jsonstring.endsWith("?"))
                    // dann entferne das Fragezeichen und Werte den JSON String aus
                    parseJSONData(jsonstring.dropLast(1))
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Log.d(TAG, e.localizedMessage!!)
        }
    }

    private fun parseJSONData(jsonString : String) {
        try {
            //response String zu einem JSON Objekt
            val obj = JSONObject(jsonString)
            //val ledstatus = obj.getString("ledstatus")
            tvData.text = obj.getString("ledstatus")

            //Array Ausgabe
            val potiArray = obj.getJSONArray("potiArray")
            tvArray.text = potiArray.toString()
        } catch (e : JSONException) {
            e.printStackTrace()
        }
    }


    private fun sendBTMessage(send: String) {
        try {
            mSocket!!.outputStream.write(send.toByteArray())
        } catch (e: IOException) {
            e.printStackTrace()
            Log.d(TAG, e.localizedMessage!!)
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

    override fun onDestroy() {
        super.onDestroy()
        dataStop()
        mSocket?.close()
    }
}