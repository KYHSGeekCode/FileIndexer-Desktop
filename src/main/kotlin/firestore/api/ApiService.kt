package firestore.api

import retrofit2.http.POST

val url = "https://fileindexer-kpdosfbnyq-du.a.run.app/"

interface ApiService {
    @POST("auth/login/")
    suspend fun login()
}