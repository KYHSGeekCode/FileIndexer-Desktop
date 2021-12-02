import com.google.api.services.drive.Drive
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.sql.PreparedStatement

data class FileRow(
    val id: Int,
    val filename: String,
    val tag: String?,
    val major_drive: String?,
    val minor_drive: String?,
    val path: String?
)

class ViewModel(val viewModelScope: CoroutineScope) {

    val defaultUserId = "user"
    val dbManager = DBManager()
//    val patternSearch: PreparedStatement

    fun find(query: String) {
        if (query.length > 1) {
            _uiState.value = UiState.Loading()
            val resultList = dbManager.find(query)
            _uiState.value = UiState.Find(query, resultList)
        } else {
            _uiState.value = UiState.Error("Please enter query with length > 1")
        }
    }

//    fun fetch() {
//        drive?.let {
//            DriveHelper.downloadDB(it, DBFile)
//        }
//    }

    fun index(major_drive: String, path: String) {
        _uiState.value = UiState.Loading()
        val addIndex = dbManager.beginIndex()
        val minor_drive = ""
        val root = File(path)
        indexFiles(root, addIndex, major_drive, minor_drive)
        dbManager.finishIndex(addIndex)
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

//    fun upload() {
//        drive?.let {
//            DriveHelper.uploadDB(it, DBFile)
//        }
//    }

    fun close() {
        dbManager.close()
        trySyncDB()
    }

    fun loginGoogleDrive(userId: String = defaultUserId) = viewModelScope.launch {
        val drive = DriveHelper.login(userId)
        _currentAccount.value = drive.About().get().setFields("user").execute().user.emailAddress
        this@ViewModel.drive = drive
    }


    fun logoutGoogleDrive() {
        DriveHelper.logout()
        drive = null
        _currentAccount.value = null
    }

    fun indexGoogleDrive() {
        drive?.apply {
            Thread {
                val addIndex = dbManager.beginIndex()
                val major_drive = "Drive"
                val minor_drive = currentAccount.value
                DriveHelper.indexFiles(this, onFileFound = { file ->
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
                dbManager.finishIndex(addIndex)
            }.start()
        }
    }

    suspend fun silentLogin(): Job? {
        if (DriveHelper.isSilentLoginAvailable()) {
            return loginGoogleDrive()
        }
        return null
    }

    // try downloading db from drive.
    // if local does not exist, use it.
    // if local exists, use the newer one.
    // if not using remote, upload local to drive.
    // TODO: lock/synchronize
    fun trySyncDB() {
        drive?.apply {
            _syncState.value = SyncState.Loading
            _syncState.value = dbManager.syncDB(this@apply)
        }
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState = _uiState as StateFlow<UiState>

    private val _currentAccount = MutableStateFlow<String?>(null)
    val currentAccount = _currentAccount as StateFlow<String?>

    private var drive: Drive? = null

    private val _currentProgress = MutableStateFlow(0.0f)
    val currentProgress = _currentProgress as StateFlow<Float>

    private val _syncState = MutableStateFlow<SyncState>(SyncState.None)
    val syncState = _syncState as StateFlow<SyncState>

}