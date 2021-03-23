package com.example.simplebluetooth

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import splitties.toast.toast


class MainActivity : AppCompatActivity() {

    private val buttonPairedDevices : Button by lazy{ findViewById(R.id.buttonPairedDevices) }
    private val listview : ListView by lazy{ findViewById(R.id.listview) }

    private lateinit var mBluetooth : BluetoothAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mBluetooth = BluetoothAdapter.getDefaultAdapter()
        if(mBluetooth == null)
        {
            toast(getString(R.string.bt_not_available))
            finish()
        }

        buttonPairedDevices.setOnClickListener {
            getPairedDevices()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!mBluetooth.isEnabled) {
            val turnBTOn = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(turnBTOn, 1)
        }
    }

    private fun getPairedDevices() {

        val pairedDevices = mBluetooth.bondedDevices
        val list = ArrayList<Any>()

        if (pairedDevices.size > 0) {
            for (bt in pairedDevices) {
                list.add("""${bt.name}${bt.address}""".trimIndent())
            }
        } else {
            toast(getString(R.string.bt_no_paired_devices))
        }

        val adapter: ArrayAdapter<*> = ArrayAdapter(this,
                R.layout.support_simple_spinner_dropdown_item,
                list)
        listview.adapter = adapter
    }
}