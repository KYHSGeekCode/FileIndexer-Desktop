package firestore.api

import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.HttpURLConnection
import java.net.SocketException
import java.util.concurrent.TimeUnit

var authToken: String = ""
val httpClient = OkHttpClient.Builder()
    .connectTimeout(60, TimeUnit.SECONDS)
    .readTimeout(60, TimeUnit.SECONDS)
    .writeTimeout(60, TimeUnit.SECONDS)
    .addInterceptor {
    val newRequest = it.request().newBuilder()
    val realNewRequest = if (authToken.isNotEmpty()) {
        newRequest.addHeader("Authorization", "Bearer $authToken")
    } else {
        newRequest
    }.build()
    try {
        it.proceed(realNewRequest)
    } catch (e: SocketException) {
        okhttp3.Response.Builder()
            .request(it.request())
            .protocol(Protocol.HTTP_1_1)
            .code(999)
            .message("socketException")
            .body("{${e}}".toResponseBody(null)).build()
    }
}.build()

private val retrofit: Retrofit = Retrofit.Builder()
    .baseUrl(FILEINDEXER_API_BASE_URL)
    .addConverterFactory(GsonConverterFactory.create())
    .client(httpClient)
    .build()

object FileIndexerRestApi {
    val retrofitService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }

    suspend fun login(username: String, password: String): Int {
        println("Username: $username, password: $password")
        val tok = try {
            retrofitService.login(LoginCredential(username, password))
        } catch (e: HttpException) {
            e.printStackTrace()
            return e.code()
        }
        authToken = tok.access_token
        return HttpURLConnection.HTTP_OK
    }
}