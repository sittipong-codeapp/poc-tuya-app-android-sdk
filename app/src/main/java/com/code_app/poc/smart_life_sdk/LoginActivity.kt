package com.code_app.poc.smart_life_sdk

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.tuya.smart.android.common.utils.ValidatorUtil
import com.tuya.smart.android.user.api.ILoginCallback
import com.tuya.smart.android.user.bean.User
import com.tuya.smart.home.sdk.TuyaHomeSdk
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (TuyaHomeSdk.getUserInstance().isLogin) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        setContentView(R.layout.activity_login)

        setupView()
    }

    private fun setupView() {
        btnLogin.setOnClickListener {
            val strAccount = etAccount.text.toString()
            val strCountryCode = etCountryCode.text.toString()
            val strPassword = etPassword.text.toString()
            val callback =  object : ILoginCallback {
                override fun onSuccess(user: User?) {
                    Toast.makeText(
                        this@LoginActivity,
                        "Login success",
                        Toast.LENGTH_LONG
                    ).show()

                    startActivity(
                        Intent(
                            this@LoginActivity,
                            MainActivity::class.java
                        )
                    )
                }

                override fun onError(code: String?, error: String?) {
                    Toast.makeText(
                        this@LoginActivity,
                        "login error->$error",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            if (ValidatorUtil.isEmail(strAccount)) {
                TuyaHomeSdk.getUserInstance()
                    .loginWithEmail(strCountryCode, strAccount, strPassword, callback)
            } else {
                TuyaHomeSdk.getUserInstance()
                    .loginWithPhonePassword(strCountryCode, strAccount, strPassword, callback)
            }
        }
    }
}