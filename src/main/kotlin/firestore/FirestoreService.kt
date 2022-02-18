package firestore

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface FirestoreService {
    @POST("accounts:signInWithPassword")
    suspend fun signInWithPassword(
        @Query("key") apiKey: String = API_KEY,
        @Body payload: SignInWithPasswordPayload
    ): SignInWithPasswordResponse
}

val BASE_URL = "https://identitytoolkit.googleapis.com/v1/"
val API_KEY = "AIzaSyDM5SLJhmsoA65QeOXpcQHHoI-my-tEnsw"