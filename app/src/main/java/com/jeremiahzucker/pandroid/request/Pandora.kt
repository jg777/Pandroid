package com.jeremiahzucker.pandroid.request

import com.jeremiahzucker.pandroid.BuildConfig
import com.jeremiahzucker.pandroid.PandroidApplication.Companion.Preferences
import com.jeremiahzucker.pandroid.crypt.http.EncryptionInterceptor
import com.jeremiahzucker.pandroid.request.json.v5.model.ResponseModel
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Streaming
import retrofit2.http.Url

/**
 * Created by jzucker on 6/30/17.
 * https://tuner.pandora.com/services/json/
 */
class Pandora private constructor(protocol: Protocol = Protocol.HTTPS) {

    enum class Protocol {
        HTTP,
        HTTPS;

        fun getProtocolString(): String {
            return name.toLowerCase() + "://"
        }
    }

    // Create companion object to hold constants relative to this domain
    companion object {
        private const val PANDORA_API_BASE_URI = "tuner.pandora.com/services/json/"
        val HTTP = Pandora()
        val HTTPS = Pandora(Protocol.HTTPS)
        fun RequestBuilder(method: BaseMethod, pandora: Pandora = HTTPS) = pandora.RequestBuilder(method)
        fun RequestBuilder(method: String, pandora: Pandora = HTTPS) = pandora.RequestBuilder(method)
        fun download(file: String) = HTTPS.API.attemptDownload(file).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
    }

    private val PANDORA_API_BASE_URL = protocol.getProtocolString() + PANDORA_API_BASE_URI

    // Retrofit2 interface
    private interface PandoraAPI {
        @POST("./")
        fun attemptPOST(
            @Query(value = "method") method: String,
            @Query(value = "partner_id") partnerId: String?,
            @Query(value = "auth_token") authToken: String?,
            @Query(value = "user_id") userId: String?,
            @Header(value = EncryptionInterceptor.ENC_HEADER_TAG) encrypted: Boolean,
            @Body body: Any?
        ): Observable<ResponseModel>

        @Streaming
        @GET
        fun attemptDownload(@Url file: String): Observable<ResponseBody>
    }

    private val API: PandoraAPI by lazy {
        Retrofit.Builder()
            .baseUrl(PANDORA_API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build()
            .create(PandoraAPI::class.java)
    }

    private val client: OkHttpClient by lazy {
        val builder = OkHttpClient.Builder()

        // Ensure the logging interceptor is in the chain before the encryption interceptor
        if (BuildConfig.DEBUG) {
            val loggingInterceptor = HttpLoggingInterceptor()
            loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
            builder.addInterceptor(loggingInterceptor)
        }

        val encryptionInterceptor = EncryptionInterceptor()
        builder.addInterceptor(encryptionInterceptor)
        builder.build()
    }

    inner class RequestBuilder(private var method: String) {
        private var partnerId: String? = Preferences.partnerId
        private var authToken: String? = Preferences.userAuthToken
        private var userId: String? = Preferences.userId
        private var encrypted: Boolean = true
        private var body: Any? = null

        constructor(method: BaseMethod) : this(method.methodName)

        fun method(method: BaseMethod): RequestBuilder {
            this.method = method.methodName
            return this
        }

        fun method(method: String): RequestBuilder {
            this.method = method
            return this
        }

        fun partnerId(partnerId: String?): RequestBuilder {
            this.partnerId = partnerId
            return this
        }

        fun authToken(authToken: String?): RequestBuilder {
            this.authToken = authToken
            return this
        }

        fun userId(userId: String?): RequestBuilder {
            this.userId = userId
            return this
        }

        fun encrypted(encrypted: Boolean): RequestBuilder {
            this.encrypted = encrypted
            return this
        }

        fun body(body: Any?): RequestBuilder {
            this.body = body
            return this
        }

        fun buildResponseModel(): Observable<ResponseModel> = API.attemptPOST(
            method = method,
            partnerId = partnerId,
            authToken = authToken,
            userId = userId,
            encrypted = encrypted,
            body = body
        ).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())

        inline fun <reified T> build(): Observable<T> = buildResponseModel()
//                .filter { it.isOk }
            .map {
                it.code == 1001 && throw InvalidAuthException(it.code.toString())
                it.getResult<T>()
            }
    }

    class InvalidAuthException(msg: String) : Exception(msg)
}
