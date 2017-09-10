package com.jeremiahzucker.pandroid.ui.launch

import android.os.Bundle
import android.content.Intent
import android.util.Log
import com.jeremiahzucker.pandroid.Preferences
import com.jeremiahzucker.pandroid.ui.auth.AuthActivity
import com.jeremiahzucker.pandroid.request.BasicCallback
import com.jeremiahzucker.pandroid.request.Pandora
import com.jeremiahzucker.pandroid.crypt.BlowFish
import com.jeremiahzucker.pandroid.request.method.auth.PartnerLogin
import com.jeremiahzucker.pandroid.request.model.ResponseModel
import com.jeremiahzucker.pandroid.ui.base.BaseActivity
import com.jeremiahzucker.pandroid.util.hexStringToByteArray
import retrofit2.Call


class LaunchActivity : BaseActivity() {

    val TAG = "LaunchActivity"
    var partnerLoginCall: Call<ResponseModel>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Preferences.reset()
        doPartnerLogin()
    }

    fun decryptSyncTime(raw: String): String {
        val fugu = BlowFish()
        val decoded = raw.hexStringToByteArray()
        var decrypted = fugu.decrypt(decoded)

        decrypted = decrypted.copyOfRange(4, decrypted.size)

        return String(decrypted, Charsets.UTF_8)
    }

    fun doPartnerLogin() {
        if (partnerLoginCall == null) {
            Log.i(TAG, "Creating Call")
            partnerLoginCall = Pandora().RequestBuilder(PartnerLogin)
                    .body(PartnerLogin.RequestBody())
                    .encrypted(false)
                    .build()

            Log.i(TAG, "Making Call")
            partnerLoginCall?.enqueue(object : BasicCallback<ResponseModel>() {
                override fun handleSuccess(responseModel: ResponseModel) {
                    val result = responseModel.getResult<PartnerLogin.ResponseBody>()
                    if (responseModel.isOk && result != null) {
                        Log.i(TAG, "Handling success")
                        // Following Pithos impl. Differs from docs.
                        Preferences.syncTimeOffset = decryptSyncTime(result.syncTime).toLong() - (System.currentTimeMillis() / 1000L)
                        Preferences.partnerId = result.partnerId
                        Preferences.partnerAuthToken = result.partnerAuthToken
                        goToMain()
                    } else {
                        handleCommonError()
                    }
                }

                override fun handleConnectionError() {
                    Log.e(TAG, "Connection Error")
                }

                override fun handleStatusError(responseCode: Int) {
                    Log.e(TAG, responseCode.toString())
                }

                override fun handleCommonError() {
                    Log.e(TAG, "Common Error")
                }

                override fun onFinish() {
                    Log.i(TAG, "Call finished")
                    partnerLoginCall = null
                }

            })
        }
    }

    fun goToMain() {
        val intent = Intent(this, AuthActivity::class.java)
        startActivity(intent) // should probs use no history
        finish()
    }
}
