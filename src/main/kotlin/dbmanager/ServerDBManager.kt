package dbmanager

import FileRow
import RecordModel
import SyncState
import com.toxicbakery.logging.Arbor
import firestore.api.ApiService
import firestore.api.FileIndexerRestApi
import kotlinx.coroutines.Job
import java.time.LocalDateTime
import firestore.api.Record
import java.io.IOException


class ServerDBManager(private val apiService: ApiService) : DBManager {
    enum class ManagerState {
        Clean,
        Indexing,
    }

    private var managerState: ManagerState = ManagerState.Clean
    val recordsToAdd = mutableListOf<Record>()
    override suspend fun login(username: String, password: String) : Int {
        return FileIndexerRestApi.login(username, password)
    }

    override fun trySilentLogin(): Job? {
        return null
    }

    override suspend fun find(query: String): List<FileRow> {
        return apiService.search(query).map {
            FileRow(
                filename = it.filename,
                tag = it.tag,
                major_drive = it.major_drive,
                minor_drive = it.minor_drive,
                id = 0,
                path = it.path
            )
        }
    }

    override suspend fun syncDB(): SyncState {
        return SyncState.Success(
            merge = false,
            upload = true,
            lastModifiedDrive = null,
            lastModifiedLocal = LocalDateTime.now(),
            date = LocalDateTime.now()
        )
    }

    override fun beginIndex() {
        managerState = ManagerState.Indexing
        recordsToAdd.clear()
    }

    override fun addRecord(record: RecordModel) {
        require(managerState == ManagerState.Indexing) {
            print("You should call beginIndex() first")
        }
        recordsToAdd.add(Record(record))
    }

    override suspend fun finishIndex() {
        require(managerState == ManagerState.Indexing) {
            print("You should call beginIndex() first")
        }
        if (recordsToAdd.size > 1000) {
            throw IOException("Too many records")
        }
        Arbor.e("Will record ${recordsToAdd.size} records")
        apiService.postRecords(recordsToAdd)
        managerState = ManagerState.Clean
    }

    override fun close() {
    }
}