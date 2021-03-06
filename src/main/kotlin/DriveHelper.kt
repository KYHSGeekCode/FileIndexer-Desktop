import DriveHelper.HTTP_TRANSPORT
import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.FileContent
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.DateTime
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.google.api.services.drive.model.FileList
import java.io.*


object DriveHelper {
    private val SCOPES = listOf(DriveScopes.DRIVE_METADATA_READONLY, DriveScopes.DRIVE_APPDATA)
    private const val CREDENTIALS_FILE_PATH = "/credentials.json"
    private const val APPLICATION_NAME = "File Indexer"
    private val JSON_FACTORY: JsonFactory = JacksonFactory.getDefaultInstance()
    private const val TOKENS_DIRECTORY_PATH = "tokens"
    val HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport()

    fun logout() {
        File(TOKENS_DIRECTORY_PATH).deleteRecursively()
    }

    /**
     * Creates an authorized Credential object.
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    @Throws(IOException::class)
    fun getCredentials(userid: String): Credential? {
        // Load client secrets.
        val clientSecretIs: InputStream = DriveHelper::class.java.getResourceAsStream(CREDENTIALS_FILE_PATH)
            ?: throw FileNotFoundException("Resource not found: $CREDENTIALS_FILE_PATH")
        val clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, InputStreamReader(clientSecretIs))

        // Build flow and trigger user authorization request.
        val flow = GoogleAuthorizationCodeFlow.Builder(
            HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES
        )
            .setDataStoreFactory(FileDataStoreFactory(File(TOKENS_DIRECTORY_PATH)))
            .setAccessType("offline")
            .build()
        val receiver = LocalServerReceiver.Builder().setPort(8888).build()
        return AuthorizationCodeInstalledApp(flow, receiver).authorize(userid)
    }

    fun indexFiles(service: Drive, onFileFound: (File) -> Unit, progressListener: (Long, Long) -> Boolean) {
        val totalSiz = service.About().get().setFields("storageQuota").execute().storageQuota.usageInDrive
        var sizSum = 0L
        println("TotalSiz: $totalSiz")
        var pageToken: String? = null
        do {
            val result = service.files().list()
                .setFields("nextPageToken, files(id, name, size, webViewLink)")
                .setQ(
                    "mimeType != 'application/vnd.google-apps.folder' " +
                            "and mimeType != 'application/vnd.google-apps.shortcut' " +
                            "and not 'appDataFolder' in parents"
                ) // application/vnd.google-apps.shortcut
                .setPageToken(pageToken)
                .execute()
            val files: List<File>? = result.files
            if (files == null || files.isEmpty()) {
                println("No files found.")
            } else {
                println("Files:")
                for (file in files) {
                    System.out.printf("%s (%s)\n", file.name, file.id)
                    onFileFound(file)
                    sizSum += file.getSize() ?: 0
                }
                if (!progressListener(totalSiz, sizSum)) {
                    break
                }
                pageToken = result.nextPageToken
            }
        } while (pageToken != null)
    }

    fun login(userId: String): Drive = // Build a new authorized API client service.
        Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(userId))
            .setApplicationName(APPLICATION_NAME)
            .build()

    fun uploadDB(service: Drive, dbFile: java.io.File): String {
        val fileMetadata = File()
        fileMetadata.name = "fileindex.sqlite"
        fileMetadata.parents = listOf("appDataFolder")
        val mediaContent = FileContent("application/vnd.sqlite3", dbFile)
        val file: File = service.files().create(fileMetadata, mediaContent)
            .setFields("id")
            .execute()
        println("File ID: " + file.id)
        return file.id
    }

    fun downloadDB(service: Drive, downloadTo: java.io.File): DateTime? {
        val files: FileList = service.files().list()
            .setSpaces("appDataFolder")
            .setFields("nextPageToken, files(id, name, modifiedTime)")
            .setQ("name = 'fileindex.sqlite'")
            .setPageSize(10)
            .execute()
        for (file in files.files) {
            System.out.printf(
                "Found file: %s (%s)\n",
                file.name, file.id
            )
            val outputStream: OutputStream = FileOutputStream(downloadTo)
            service.files().get(file.id).executeMediaAndDownloadTo(outputStream)
            return file.modifiedTime
        }
        return null
    }

    fun isSilentLoginAvailable(): Boolean {
        val tokenDir = File(TOKENS_DIRECTORY_PATH)
        return tokenDir.exists() && tokenDir.isDirectory && !tokenDir.listFiles().isNullOrEmpty()
    }
}