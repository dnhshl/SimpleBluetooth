package com.example.simplebluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import splitties.toast.toast


class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"

    companion object {
        const val EXTRA_ADDRESS = "device_address"
    }

    private val buttonPairedDevices : Button by lazy{ findViewById(R.id.buttonPairedDevices) }
    private val listview : ListView by lazy{ findViewById(R.id.listview) }
    private val buttonDiscoverDevices : Button by lazy{ findViewById(R.id.buttonDiscoverDevices) }

    private lateinit var mBluetooth : BluetoothAdapter

    private var discoveredDevices = arrayListOf<String>()

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

        buttonDiscoverDevices.setOnClickListener {
            checkBTPermission()
            getDiscoverDevices()
        }

        listview.onItemClickListener = lvClickListener
    }

    private val lvClickListener = AdapterView.OnItemClickListener {
        parent, view, position, id ->
        val info = (view as TextView).text.toString()
        val address = info.substring(info.length - 17)
        val i = Intent(this@MainActivity, BTControl::class.java)
        i.putExtra(EXTRA_ADDRESS, address)
        startActivity(i)
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

    private fun checkBTPermission() {
        var permissionCheck = checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION")
        permissionCheck += checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION")
        if (permissionCheck != 0) {
            requestPermissions(arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION), 1001)
        }
    }

    private fun getDiscoverDevices() {
        if(!mBluetooth.isDiscovering) { // Suche ist nicht gestartet
            mBluetooth.startDiscovery()  // starte Suche
            val discoverDevicesIntent = IntentFilter(BluetoothDevice.ACTION_FOUND) //auf diese Signale soll unser Broadcast Receiver filtern
            registerReceiver(mBroadcastReceiver, discoverDevicesIntent)
            buttonDiscoverDevices.text = getString(R.string.stop_search_device);
        } else {                        // Suche ist gestartet
            mBluetooth.cancelDiscovery() // Stoppe suche
            unregisterReceiver(mBroadcastReceiver);
            buttonDiscoverDevices.text = getString(R.string.start_search_device);
        }
    }

    private val mBroadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val action = intent.action
            if (action == BluetoothDevice.ACTION_FOUND) {
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                val deviceInfo = """${device!!.name}${device.address}""".trimIndent()


                Log.i(TAG, deviceInfo)

                // gefundenes Gerät der Liste hinzufügen, wenn es noch nicht aufgeführt ist
                if (!discoveredDevices.contains(deviceInfo)) {
                    discoveredDevices.add(deviceInfo)
                }

                // aktualisierte Liste im Listview anzeigen
                val adapt = ArrayAdapter(applicationContext, android.R.layout.simple_list_item_1, discoveredDevices)
                listview.adapter = adapt
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(mBroadcastReceiver)
        mBluetooth.cancelDiscovery()
    }
}