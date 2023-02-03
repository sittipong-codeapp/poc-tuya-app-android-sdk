package com.code_app.poc.smart_life_sdk

import android.app.Application
import com.tuya.smart.home.sdk.TuyaHomeSdk

class BaseApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        TuyaHomeSdk.init(this)
        TuyaHomeSdk.setDebugMode(true)
    }

    override fun onTerminate() {
        super.onTerminate()

        TuyaHomeSdk.onDestroy()
    }
}