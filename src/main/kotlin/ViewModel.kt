import dbmanager.DBManager
import com.google.api.services.drive.Drive
import com.toxicbakery.logging.Arbor
import firestore.api.authToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

data class FileRow(
    val id: Int,
    val filename: String,
    val tag: String?,
    val major_drive: String?,
    val minor_drive: String?,
    val path: String?
)

class ViewModel(val viewModelScope: CoroutineScope, val dbManager: DBManager) {
    private var drive: Drive? = null
    val defaultUserId = "user"

    fun find(query: String) {
        if (query.length > 1) {
            viewModelScope.launch(Dispatchers.IO) {
                _uiState.value = UiState.Loading()
                try {
                    val resultList = dbManager.find(query)
                    _uiState.value = UiState.Find(query, resultList)
                } catch (e: Exception) {
                    _uiState.value = UiState.Error("Error: ${e.stackTraceToString()}")
                }
            }
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
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = UiState.Loading()
            dbManager.beginIndex()
            val minor_drive = ""
            val root = File(path)
            indexFiles(root, major_drive, minor_drive)
            dbManager.finishIndex()
            _uiState.value = UiState.Idle
        }
    }

    private fun indexFiles(
        root: File,
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
                val record = RecordModel(fileName, "", major_drive, minor_drive, path)
                dbManager.addRecord(record)

                if (!isIgnoreSub && file.isDirectory) {
                    _uiState.value = UiState.Loading(0.0f, "Indexing ${file.path}")
                    indexFiles(file, major_drive, minor_drive)
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
            viewModelScope.launch(Dispatchers.IO) {
                dbManager.beginIndex()
                val major_drive = "Drive"
                val minor_drive = currentAccount.value
                DriveHelper.indexFiles(this@apply, onFileFound = { file ->
                    dbManager.addRecord(
                        RecordModel(file.name, "", major_drive, minor_drive ?: "", file.webViewLink)
                    )
                }) { total, current ->
                    val pcnt = current.toFloat() / total
                    println("Total: $total, current: $current, pcnt: $pcnt")
                    _currentProgress.value = pcnt
                    true
                }
                dbManager.finishIndex()
            }
        }
    }

    fun silentLogin(): Job? {
        return dbManager.trySilentLogin()
    }

    // try downloading db from drive.
    // if local does not exist, use it.
    // if local exists, use the newer one.
    // if not using remote, upload local to drive.
    // TODO: lock/synchronize
    fun trySyncDB() {
        viewModelScope.launch(Dispatchers.IO) {
            _syncState.value = SyncState.Loading
            _syncState.value = dbManager.syncDB()
            _syncState.value = SyncState.None
        }
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            dbManager.login(username, password)
            Arbor.e("Login success $authToken")
        }
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState = _uiState as StateFlow<UiState>

    private val _currentAccount = MutableStateFlow<String?>(null)
    val currentAccount = _currentAccount as StateFlow<String?>

    private val _currentProgress = MutableStateFlow(0.0f)
    val currentProgress = _currentProgress as StateFlow<Float>

    private val _syncState = MutableStateFlow<SyncState>(SyncState.None)
    val syncState = _syncState as StateFlow<SyncState>
}