package firestore.api

import RecordModel
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

val FILEINDEXER_API_BASE_URL = "https://fileindexer-kpdosfbnyq-du.a.run.app/"

data class LoginCredential(
    val username: String,
    val password: String
)

data class AccessToken(
    val access_token: String
)

data class Record(
    val filename: String,
    val tag: String,
    val major_drive: String,
    val minor_drive: String,
    val path: String
) {
    constructor(recordModel: RecordModel) : this(
        recordModel.fileName,
        recordModel.tag,
        recordModel.major_drive,
        recordModel.minor_drive,
        recordModel.path
    )
}

data class RecordParsed(
    val filename: String,
    val tag: String,
    val major_drive: String,
    val minor_drive: String,
    val path: String,
    val nlp_parsed: List<String>
)


interface ApiService {
    @POST("auth/login/")
    suspend fun login(@Body credential: LoginCredential): AccessToken

    @POST("record/")
    suspend fun postRecord(@Body record: Record): RecordParsed // With nlp_parsed array

    @POST("records/")
    suspend fun postRecords(@Body records: List<Record>): List<RecordParsed> // With nlp_parsed array

    @GET("record/")
    suspend fun search(@Query("query") query: String): List<Record>
}