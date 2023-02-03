package com.code_app.poc.smart_life_sdk

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import com.tuya.smart.home.sdk.TuyaHomeSdk
import com.tuya.smart.home.sdk.builder.TuyaGwSubDevActivatorBuilder
import com.tuya.smart.sdk.api.ITuyaActivator
import com.tuya.smart.sdk.api.ITuyaSmartActivatorListener
import com.tuya.smart.sdk.bean.DeviceBean
import kotlinx.android.synthetic.main.activity_device_adding_zigbee_subdevice.*

class DeviceAddingZigbeeSubdeviceActivity : AppCompatActivity() {
    companion object {
        const val TAG = "DeviceConfigZBSubdevice"
        const val INTENT_GATEWAY_ID = "INTENT_GATEWAY_ID"
        const val INTENT_GATEWAY_NAME = "INTENT_GATEWAY_NAME"
    }

    private var gatewayId: String? = null
    private var gatewayName: String? = null
    private var tuyaActivator: ITuyaActivator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_adding_zigbee_subdevice)
        title = "Zigbee Sub-devices"

        setupData()
        setupView()
        setupSearchButton()
    }

    override fun onDestroy() {
        tuyaActivator?.stop()
        tuyaActivator?.onDestroy()

        super.onDestroy()
    }

    private fun setupData() {
        with (intent) {
            gatewayId = getStringExtra(INTENT_GATEWAY_ID)
            gatewayName = getStringExtra(INTENT_GATEWAY_NAME)
        }
    }

    private fun setupView() {
        gateway_text_view.text = gatewayName ?: gatewayId
    }

    private fun setupSearchButton() {
        search_button.setOnClickListener {
            hideKeyboard(it)
            showLoadingIndicator()
            pairDevice()
        }
    }

    private fun pairDevice() {
        val gatewayId = this.gatewayId
        if (gatewayId.isNullOrEmpty()) {
            Toast.makeText(
                this@DeviceAddingZigbeeSubdeviceActivity,
                "getActivatorToken error->Invalid gateway id: $gatewayId",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        val builder = TuyaGwSubDevActivatorBuilder()
            .setDevId(gatewayId)
            .setTimeOut(100)
            .setListener(object : ITuyaSmartActivatorListener {
                override fun onStep(step: String?, data: Any?) {
                    step?.let { step -> pairing_status_text_view.text = step }
                    Log.i(TAG, "$step --> $data")
                }

                override fun onActiveSuccess(devResp: DeviceBean?) {
                    pairing_status_layout.visibility = View.GONE

                    Log.i(TAG, "Activate success")
                    Toast.makeText(
                        this@DeviceAddingZigbeeSubdeviceActivity,
                        "Activate success",
                        Toast.LENGTH_LONG
                    ).show()

                    setResult(RESULT_OK)
                    finish()
                }

                override fun onError(errorCode: String?, errorMsg: String?) {
                    pairing_status_layout.visibility = View.GONE
                    search_button.isClickable = true

                    Toast.makeText(
                        this@DeviceAddingZigbeeSubdeviceActivity,
                        "activator error->$errorCode $errorMsg",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })

        pairing_status_text_view.text = "Binding..."

        tuyaActivator = TuyaHomeSdk.getActivatorInstance()
            .newGwSubDevActivator(builder)
            .also { activator -> activator.start() }
    }

    private fun hideKeyboard(view: View) {
        val imm: InputMethodManager =
            getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun showLoadingIndicator() {
        pairing_status_text_view.text = "Loading..."
        pairing_status_layout.visibility = View.VISIBLE
        search_button.isClickable = false
    }
}