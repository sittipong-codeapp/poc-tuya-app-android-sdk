package com.code_app.poc.smart_life_sdk

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import com.tuya.smart.android.hardware.bean.HgwBean
import com.tuya.smart.home.sdk.TuyaHomeSdk
import com.tuya.smart.home.sdk.builder.TuyaGwActivatorBuilder
import com.tuya.smart.sdk.api.ITuyaActivator
import com.tuya.smart.sdk.api.ITuyaActivatorGetToken
import com.tuya.smart.sdk.api.ITuyaSmartActivatorListener
import com.tuya.smart.sdk.bean.DeviceBean
import kotlinx.android.synthetic.main.activity_device_adding_zigbee_gateway.*

class DeviceAddingZigbeeGatewayActivity : AppCompatActivity() {
    companion object {
        const val TAG = "DeviceAddingZBGW"
        const val INTENT_HOME_ID = "INTENT_HOME_ID"
    }

    private var homeId: Long = 0
    private var tuyaToken: String? = null
    private var tuyaActivator: ITuyaActivator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_adding_zigbee_gateway)
        title = "Zigbee Gateway"

        setupData()
        getTuyaToken()
        setupSearchButton()
    }

    override fun onDestroy() {
        tuyaActivator?.stop()
        tuyaActivator?.onDestroy()

        super.onDestroy()
    }

    private fun setupData() {
        with (intent) {
            homeId = getLongExtra(INTENT_HOME_ID, 0)
        }
    }

    private fun getTuyaToken() {
        if (homeId == 0L) {
            Toast.makeText(
                this@DeviceAddingZigbeeGatewayActivity,
                "getActivatorToken error->Invalid home id: $homeId",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        TuyaHomeSdk.getActivatorInstance().getActivatorToken(homeId, object :
            ITuyaActivatorGetToken {
            override fun onSuccess(token: String?) {
                this@DeviceAddingZigbeeGatewayActivity.tuyaToken = token
                updateData()
            }

            override fun onFailure(errorCode: String?, errorMsg: String?) {
                Toast.makeText(
                    this@DeviceAddingZigbeeGatewayActivity,
                    "getActivatorToken error->$errorCode $errorMsg",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }

    private fun setupSearchButton() {
        search_button.setOnClickListener {
            hideKeyboard(it)
            showLoadingIndicator()
            searchGatewayDevice()
        }
    }

    private fun updateData() {
        tuya_token_text_view.text = tuyaToken
    }

    private fun searchGatewayDevice() {
        val newSearcher = TuyaHomeSdk.getActivatorInstance().newTuyaGwActivator().newSearcher()

        pairing_status_text_view.text = "Scanning..."

        // TODO: Find the way to handle a `device not found` case.
        newSearcher.registerGwSearchListener {
            pairDevice(it)
        }
    }

    private fun pairDevice(hgwBean: HgwBean) {
        val tuyaToken = this.tuyaToken

        if (tuyaToken.isNullOrEmpty()) return

        val builder = TuyaGwActivatorBuilder()
            .setContext(this@DeviceAddingZigbeeGatewayActivity)
            .setToken(tuyaToken)
            .setHgwBean(hgwBean)
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
                        this@DeviceAddingZigbeeGatewayActivity,
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
                        this@DeviceAddingZigbeeGatewayActivity,
                        "activator error->$errorCode $errorMsg",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })

        pairing_status_text_view.text = "Binding..."

        tuyaActivator = TuyaHomeSdk.getActivatorInstance()
            .newGwActivator(builder)
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