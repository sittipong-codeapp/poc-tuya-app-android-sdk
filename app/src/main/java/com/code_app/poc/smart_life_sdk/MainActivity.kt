package com.code_app.poc.smart_life_sdk

import android.bluetooth.BluetoothClass.Device
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.BaseAdapter
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.tuya.smart.android.user.api.ILogoutCallback
import com.tuya.smart.android.user.bean.User
import com.tuya.smart.home.sdk.TuyaHomeSdk
import com.tuya.smart.home.sdk.bean.HomeBean
import com.tuya.smart.home.sdk.callback.ITuyaGetHomeListCallback
import com.tuya.smart.home.sdk.callback.ITuyaHomeResultCallback
import com.tuya.smart.sdk.api.IResultCallback
import com.tuya.smart.sdk.bean.DeviceBean
import com.wdullaer.swipeactionadapter.SwipeActionAdapter
import com.wdullaer.swipeactionadapter.SwipeActionAdapter.SwipeActionListener
import com.wdullaer.swipeactionadapter.SwipeDirection
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    companion object {
        const val INTENT_REQUEST_CODE_EZ_MODE = 1001
        const val INTENT_REQUEST_CODE_AP_MODE = 1002
        const val INTENT_REQUEST_CODE_ZBGW_MODE = 1003
        const val INTENT_REQUEST_CODE_ZBSD_MODE = 1004
    }

    private var user: User? = null
    private var currentHomeBean: HomeBean? = null

    private val username get() = user?.let { it.email.split("@").firstOrNull() } ?: "Anonymous"
    private val userId get() = user?.uid ?: "-"
    private val sessionId get() = user?.sid ?: "-"
    private val currentHomeName get() = currentHomeBean?.name ?: "-"
    private val currentHomeId get() = currentHomeBean?.homeId

    private lateinit var myAdapter: MyAdapter
    private lateinit var swipeActionAdapter: SwipeActionAdapter
    private val items: List<ListViewChild> get() = listOf(
        ListViewItem(data = GreetingItemData(username = username)),
        ListViewItem(data = InformationItemData(label = "User ID", value = userId)),
        ListViewItem(data = InformationItemData(label = "Session ID", value = sessionId)),
        ListViewSectionHeader(title = "Home Management"),
        ListViewItem(data = InformationItemData(label = "Current Home", value = currentHomeName)),
        ListViewSectionHeader(title = "Device List"),
    ) + deviceItems
    private val deviceItems: List<ListViewItem<DeviceItemData>> get() = currentHomeBean
        ?.deviceList
        ?.map {
            DeviceItemData(
                id = it.devId,
                name = it.name,
                connectionStatus = if (it.isOnline) "Online" else "Offline",
                isPowerOn = true
            )
        }
        ?.map { ListViewItem(it) }
        ?: emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        user = TuyaHomeSdk.getUserInstance().user

        TuyaHomeSdk.getHomeManagerInstance().queryHomeList(object : ITuyaGetHomeListCallback {
            override fun onSuccess(homeBeans: MutableList<HomeBean>?) {
                homeBeans?.firstOrNull()?.also {
                    setCurrentHome(it)
                    getHomeDetails(homeId = it.homeId)
                }
            }

            override fun onError(errorCode: String?, error: String?) {
                Toast.makeText(
                    this@MainActivity,
                    "queryHomeList error->$error",
                    Toast.LENGTH_LONG
                ).show()
            }
        })

        myAdapter = MyAdapter(
            context = this@MainActivity,
            items = items
        )
//        list_view.adapter = myAdapter
        swipeActionAdapter = SwipeActionAdapter(myAdapter)
        swipeActionAdapter.setListView(list_view)
        list_view.adapter = swipeActionAdapter

        swipeActionAdapter.setSwipeActionListener(object : SwipeActionListener {
            override fun hasActions(position: Int, direction: SwipeDirection?): Boolean {
//                return position >= 6 && direction == SwipeDirection.DIRECTION_NORMAL_LEFT
//                return position >= 6 && direction == SwipeDirection.DIRECTION_FAR_LEFT
                return position >= 6 && direction?.isLeft ?: false
            }

            override fun shouldDismiss(position: Int, direction: SwipeDirection?): Boolean {
//                return position >= 6 && direction == SwipeDirection.DIRECTION_FAR_LEFT
                return false
            }

            override fun onSwipe(position: IntArray?, direction: Array<out SwipeDirection>?) {
                val position = position?.firstOrNull() ?: return
                val direction = direction?.firstOrNull() ?: return
                if (direction != SwipeDirection.DIRECTION_FAR_LEFT) return

                when (val item = myAdapter.getItem(position)) {
                    is ListViewItem<*> -> {
                        (item.data as? DeviceItemData)?.let { data ->
                            alertRemoveDeviceConfirmation(id = data.id, name = data.name)
                        }
                    }
                }
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main, menu)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_add_device -> {
            alertDeviceAddingOptions()
            true
        }
        R.id.action_logout -> {
            logout()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            INTENT_REQUEST_CODE_EZ_MODE,
            INTENT_REQUEST_CODE_AP_MODE,
            INTENT_REQUEST_CODE_ZBGW_MODE,
            INTENT_REQUEST_CODE_ZBSD_MODE-> if (resultCode == RESULT_OK) {
                reloadDeviceList()
            }
        }
    }

    private fun reloadDeviceList() = currentHomeId?.let { getHomeDetails(it) }

    private fun setCurrentHome(home: HomeBean) {
        currentHomeBean = home
        updateData()
    }

    private fun getHomeDetails(homeId: Long) {
        TuyaHomeSdk.newHomeInstance(homeId).getHomeDetail(object : ITuyaHomeResultCallback {
            override fun onSuccess(bean: HomeBean?) {
                bean?.also { setCurrentHome(it) }
            }

            override fun onError(errorCode: String?, error: String?) {
                Toast.makeText(
                    this@MainActivity,
                    "getHomeDetails error->$error",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }

    private fun updateData() {
        myAdapter.updateData(items = items)
        swipeActionAdapter.notifyDataSetChanged()
    }

    private fun alertRemoveDeviceConfirmation(id: String, name: String) {
        AlertDialog.Builder(this)
            .setTitle("Remove Device?")
            .setMessage(name)
            .setPositiveButton("Remove") { _, _ -> removeDevice(id = id) }
            .setNegativeButton("Cancel") { _, _ -> /* Do nothing. */ }
            .show()
    }

    private fun alertDeviceAddingOptions() {
        AlertDialog.Builder(this)
            .setTitle("Device Adding Options")
            .setItems(
                arrayOf("Wi-Fi, EZ Mode", "Wi-Fi, AP Mode", "Zigbee Gateway", "Zigbee Subdevice")
            ) { _, position ->
                when (position) {
                    0 -> openDeviceAddingEZMode()
                    1 -> openDeviceAddingAPMode()
                    2 -> openDeviceAddingZigbeeGateway()
                    3 -> openDeviceAddingZigbeeSubdevice()
                }
            }
            .show()

    }

    private fun openDeviceAddingEZMode() {
        val homeId = currentHomeId

        if (homeId == null) {
            Toast.makeText(
                this@MainActivity,
                "Error home-id not found.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val intent = Intent(this, DeviceAddingEZModeActivity::class.java)
        with(intent) {
            putExtra(DeviceAddingEZModeActivity.INTENT_HOME_ID, homeId)
        }

        startActivityForResult(intent, INTENT_REQUEST_CODE_EZ_MODE)
    }

    private fun openDeviceAddingAPMode() {
        val homeId = currentHomeId

        if (homeId == null) {
            Toast.makeText(
                this@MainActivity,
                "Error home-id not found.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val intent = Intent(this, DeviceAddingAPModeActivity::class.java)
        with(intent) {
            putExtra(DeviceAddingAPModeActivity.INTENT_HOME_ID, homeId)
        }

        startActivityForResult(intent, INTENT_REQUEST_CODE_AP_MODE)
    }

    private fun openDeviceAddingZigbeeGateway() {
        val homeId = currentHomeId

        if (homeId == null) {
            Toast.makeText(
                this@MainActivity,
                "Error home-id not found.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val intent = Intent(this, DeviceAddingZigbeeGatewayActivity::class.java)
        with(intent) {
            putExtra(DeviceAddingZigbeeGatewayActivity.INTENT_HOME_ID, homeId)
        }

        startActivityForResult(intent, INTENT_REQUEST_CODE_ZBGW_MODE)
    }

    private fun openDeviceAddingZigbeeSubdevice() {
        val gateway = currentHomeBean?.deviceList?.firstOrNull { it.isZigBeeWifi }

        if (gateway == null) {
            Toast.makeText(
                this@MainActivity,
                "Error zigbee gateway not found.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val intent = Intent(this, DeviceAddingZigbeeSubdeviceActivity::class.java)
        with(intent) {
            putExtra(DeviceAddingZigbeeSubdeviceActivity.INTENT_GATEWAY_ID, gateway.devId)
            putExtra(DeviceAddingZigbeeSubdeviceActivity.INTENT_GATEWAY_NAME, gateway.name)
        }

        startActivityForResult(intent, INTENT_REQUEST_CODE_ZBSD_MODE)
    }

    private fun removeDevice(id: String) {
        TuyaHomeSdk.newDeviceInstance(id).removeDevice(object : IResultCallback {
            override fun onSuccess() {
                currentHomeBean?.let { getHomeDetails(homeId = it.homeId) }

                Toast.makeText(
                    this@MainActivity,
                    "Delete Success",
                    Toast.LENGTH_LONG
                ).show()
            }

            override fun onError(s: String, s1: String) {
                Toast.makeText(
                    this@MainActivity,
                    "removeDevice error->$s $s1",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }

    private fun logout() {
        TuyaHomeSdk.getUserInstance().logout(object : ILogoutCallback {
            override fun onSuccess() {
                // Clear cache
//                HomeModel.INSTANCE.clear(this@MainSampleListActivity)

                // Navigate to User Func Navigation Page
                val intent = Intent(this@MainActivity, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }

            override fun onError(code: String?, error: String?) {
                Toast.makeText(
                    this@MainActivity,
                    "logout error->$error",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }
}

private class MyAdapter(
    private val context: Context,
    private var items: List<ListViewChild>,
) : BaseAdapter() {

    override fun getCount(): Int {
        return items.size
    }

    override fun getItem(position: Int): Any {
        return items[position]
    }

    override fun getItemId(position: Int): Long {
        return items[position].hashCode().toLong()
    }

    // override other abstract methods here
    override fun getView(position: Int, convertView: View?, container: ViewGroup?): View? {
//        var convertView: View? = convertView
//        if (convertView == null) {
//            val layoutInflater = LayoutInflater.from(context)
//            convertView = layoutInflater.inflate(R.layout.list_item, container, false)
//        }
//        (convertView.findViewById(android.R.id.text1) as TextView)
//            .setText(getItem(position))
//        return convertView
        val item = getItem(position)
        val layoutInflater = LayoutInflater.from(context)

        when (item) {
            is ListViewItem<*> -> {
                return (item.data as? GreetingItemData)?.let { data ->
                    val itemView = layoutInflater.inflate(R.layout.main_list_greeting_item, container, false)

                    (itemView.findViewById(R.id.greeting_text_view) as? TextView)?.let {
                        it.text = "Hello, ${data.username}!"
                    }

                    itemView
                } ?: (item.data as? InformationItemData)?.let { data ->
                    val itemView = layoutInflater.inflate(R.layout.main_list_information_item, container, false)

                    (itemView.findViewById(R.id.information_label_text_view) as? TextView)?.let {
                        it.text = data.label
                    }
                    (itemView.findViewById(R.id.information_value_text_view) as? TextView)?.let {
                        it.text = data.value
                    }

                    itemView
                } ?: (item.data as? DeviceItemData)?.let { data ->
                    val itemView = layoutInflater.inflate(R.layout.main_list_device_item, container, false)

                    (itemView.findViewById(R.id.device_name_text_view) as? TextView)?.let {
                        it.text = data.name
                    }
                    (itemView.findViewById(R.id.connection_status_text_view) as? TextView)?.let {
                        it.text = data.connectionStatus
                    }
                    (itemView.findViewById(R.id.power_switch) as? Switch)?.let {
                        it.isChecked = data.isPowerOn
                    }

                    itemView
                }
            }
            is ListViewSectionHeader -> {
                val itemView = layoutInflater.inflate(R.layout.section_header, container, false)

                (itemView.findViewById(R.id.title_text_view) as? TextView)?.let {
                    it.text = item.title
                }

                return itemView
            }
            else -> {
                return null
            }
        }
    }

    fun updateData(items: List<ListViewChild>) {
        this.items = items
        notifyDataSetChanged()
    }
}

data class DeviceItemData(
    val id: String,
    val name: String,
    val connectionStatus: String,
    val isPowerOn: Boolean
)

data class GreetingItemData(val username: String)

data class InformationItemData(
    val label: String,
    val value: String,
    val hasDetails: Boolean = false,
)

// Helper

interface ListViewChild {}

class ListViewItem<T>(val data: T) : ListViewChild

data class ListViewSectionHeader(val title: String) : ListViewChild
