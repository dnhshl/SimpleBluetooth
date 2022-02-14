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

        toast(address)

        ConnectBT().execute()

        btnBlinken.setOnClickListener {
            val obj = JSONObject()
            val arr = JSONArray()
            try {
                arr.put(0, "H")
                arr.put(1, "L")
                arr.put(2, "H")
                arr.put(3, "L")
                arr.put(4, "H")
                arr.put(5, "L")

                obj.put("array", arr)
            }catch (e: IOException) {
                e.printStackTrace()
                toast(e.localizedMessage!!)
            }
            sendBTMessage("?" + obj.toString() + "?")
        }

        btnLed.setOnClickListener {
            ledIsOn = !ledIsOn
            val obj = JSONObject()
            if (ledIsOn) {
                try {
                    obj.put("LED", "H");
                }catch (e: IOException) {
                    e.printStackTrace()
                    toast(e.localizedMessage!!)
                }
                sendBTMessage("!" + obj.toString() + "!")
                btnLed.text = getString(R.string.led_off)
            } else {
                try {
                    obj.put("LED", "L");
                }catch (e: IOException) {
                    e.printStackTrace()
                    toast(e.localizedMessage!!)
                }
                sendBTMessage("!" + obj.toString() + "!")
                btnLed.text = getString(R.string.led_on)
            }
        }

        btnData.setOnClickListener {
            receivingData = !receivingData
            dataStart()
        }
    }

    private fun dataStart() {
        if (receivingData) {
            btnData.text = getString(R.string.data_stop)
            mRunnable = Runnable {
                mHandler.postDelayed(mRunnable, 1000)
                readBuffer()
            }
            mHandler.postDelayed(mRunnable, 1000)
        } else {
            btnData.text = getString(R.string.data_start)
            if (mRunnable != null) {
                mHandler.removeCallbacks(mRunnable)
            }
        }
    }

    private fun readBuffer() {
        val buffer = ByteArray(2048)
        var builder = StringBuilder()

        try {
            val mInputStream = mSocket!!.inputStream
            val bytes = mInputStream.read(buffer)
            val message = String(buffer, 0, bytes)

            builder.append(message)
            if (builder.toString().contains("!") && builder.toString().contains("?")) {
                val msg = builder.toString()
                val output = processString(msg, "!", true)
                var finalOutput: Array<String>

                output.forEach { string ->
                    if (string.contains("?")) {
                        finalOutput = processString(string, "?", false)

                        finalOutput.forEach {
                            if (it.isNotEmpty()) {

                                parseJSONData(it)

                            }
                        }
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            toast(e.localizedMessage!!)
        }
    }

    private fun parseJSONData(jsonString : String) {
        try {
            //response String zu einem JSON Objekt
            val obj = JSONObject(jsonString)
            //extrahieren des Objektes data
            val dataObj = obj.getInt("poti")

            tvData.text = dataObj.toString()

            //Array Ausgabe
            val parentArray = obj.getJSONArray("potiArray");
            val value1 = parentArray.getInt(0);
            val value2 = parentArray.getInt(1);
            val value3 = parentArray.getInt(2);
            tvArray.text = "$value1 , $value2 , $value3"


        } catch (e : JSONException) {
            e.printStackTrace()

        }
    }

    private fun processString(msg: String, splitter: String, first: Boolean): Array<String> {
        val m1: Array<String>

        m1 = if (!msg.startsWith("!") && first) {
            val index = msg.indexOfFirst { it == '!' }
            val text = msg.substring(index, msg.length)
            text.split(splitter).toTypedArray()
        } else {
            msg.split(splitter).toTypedArray()
        }
        return m1
    }

    private fun sendBTMessage(send: String) {
        try {
            mSocket!!.outputStream.write(send.toByteArray())
            Log.i(TAG, send.toByteArray().toHexString())
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

    fun ByteArray.toHexString() : String {
        return this.joinToString("") {
            java.lang.String.format("%02x", it)
        }
    }
}