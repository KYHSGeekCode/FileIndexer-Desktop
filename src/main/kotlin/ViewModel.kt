import com.google.api.services.drive.Drive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement

data class FileRow(
    val id: Int,
    val filename: String,
    val tag: String?,
    val major_drive: String?,
    val minor_drive: String?,
    val path: String?
)

class ViewModel {
    val DBPath = "${System.getProperty("user.home")}/fileindex.sqlite"
    val DBFile = File(DBPath)

    val con: Connection
    val exactSearch: PreparedStatement
//    val patternSearch: PreparedStatement

    init {
        Class.forName("org.sqlite.JDBC")
        con = DriverManager.getConnection("jdbc:sqlite:$DBPath")
        print(con.isClosed)
        exactSearch = con.prepareStatement("SELECT * FROM files WHERE filename = ?")
//        patternSearch = con.prepareStatement("SELECT * FROM files WHERE filename LIKE ?;")
    }

    fun find(query: String) {
        _uiState.value = UiState.Loading()
        val patternSearch = con.prepareStatement("SELECT * FROM files WHERE filename LIKE ?;")
        patternSearch.setString(1, "%$query%")
        patternSearch.execute()
        val resultList = ArrayList<FileRow>()
        if (!patternSearch.isClosed) {
            val result = patternSearch.resultSet
            val fileNameIndex = result.findColumn("filename")
            val idIndex = result.findColumn("id")
            val tagIndex = result.findColumn("tag")
            val majorDriveIndex = result.findColumn("major_drive")
            val minorDriveIndex = result.findColumn("minor_drive")
            val pathIndex = result.findColumn("path")
            while (result.next()) {
                val id = result.getInt(idIndex)
                val filename = result.getString(fileNameIndex)
                val tag = result.getString(tagIndex)
                val major_drive = result.getString(majorDriveIndex)
                val minor_drive = result.getString(minorDriveIndex)
                val path = result.getString(pathIndex)
                resultList.add(FileRow(id, filename, tag, major_drive, minor_drive, path))
            }
            result.close()
        }
        patternSearch.close()
        _uiState.value = UiState.Find(query, resultList)
    }

    fun fetch() {
        drive?.let {
            DriveHelper.downloadDB(it, DBFile)
        }
    }

    fun index(path: String) {
        _uiState.value = UiState.Loading()
        val addIndex =
            con.prepareStatement("INSERT INTO files(filename, tag, major_drive, minor_drive, path) VALUES (?, ?, ?, ?, ?)")

        val major_drive = ""
        val minor_drive = ""
        val root = File(path)
        indexFiles(root, addIndex, major_drive, minor_drive)
        addIndex.clearParameters()
        addIndex.executeBatch()
        addIndex.close()
        _uiState.value = UiState.Idle
    }

    private fun indexFiles(
        root: File,
        addIndex: PreparedStatement,
        major_drive: String,
        minor_drive: String
    ) {
        val list = root.listFiles()
        if (!list.isNullOrEmpty()) {
            // check if .ignore exist
            val isIgnoreSub = File(root, ".indexignore").exists()
            for (file in list) {
                val fileName = file.name
                val path = file.canonicalPath
                addIndex.setString(1, fileName)
                addIndex.setString(2, "")
                addIndex.setString(3, major_drive)
                addIndex.setString(4, minor_drive)
                addIndex.setString(5, path)
                addIndex.addBatch()
                if (!isIgnoreSub && file.isDirectory) {
                    _uiState.value = UiState.Loading(0.0f, "Indexing ${file.path}")
                    indexFiles(file, addIndex, major_drive, minor_drive)
                }
            }
        }
    }

    fun upload() {
        drive?.let {
            DriveHelper.uploadDB(it, DBFile)
        }
    }

    fun close() {
        con.close()
    }

    fun loginGoogleDrive(userId: String) {
        val drive = DriveHelper.login(userId)
        _currentAccount.value = drive.About().get().setFields("user").execute().user.emailAddress
        this.drive = drive
    }

    fun logoutGoogleDrive() {
        DriveHelper.logout()
        drive = null
        _currentAccount.value = null
    }

    fun indexGoogleDrive() {
        drive?.let {
            Thread {
                val addIndex =
                    con.prepareStatement("INSERT INTO files(filename, tag, major_drive, minor_drive, path) VALUES (?, ?, ?, ?, ?)")
                val major_drive = "Drive"
                val minor_drive = currentAccount.value
                DriveHelper.indexFiles(it, onFileFound = { file ->
                    addIndex.setString(1, file.name)
                    addIndex.setString(2, "")
                    addIndex.setString(3, major_drive)
                    addIndex.setString(4, minor_drive)
                    addIndex.setString(5, file.webViewLink)
                    addIndex.addBatch()
                }) { total, current ->
                    val pcnt = current.toFloat() / total
                    println("Total: $total, current: $current, pcnt: $pcnt")
                    _currentProgress.value = pcnt
                    true
                }
                addIndex.clearParameters()
                addIndex.executeBatch()
                addIndex.close()
            }.start()
        }
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState = _uiState as StateFlow<UiState>

    private val _currentAccount = MutableStateFlow<String?>(null)
    val currentAccount = _currentAccount as StateFlow<String?>

    private var drive: Drive? = null

    private val _currentProgress = MutableStateFlow(0.0f)
    val currentProgress = _currentProgress as StateFlow<Float>
}