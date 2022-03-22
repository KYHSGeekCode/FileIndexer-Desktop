package dbmanager

import FileRow
import RecordModel
import SyncState
import kotlinx.coroutines.Job

interface DBManager {
    suspend fun login(username: String, password: String): Int
    fun trySilentLogin(): Job?
    suspend fun find(query: String): List<FileRow>
    suspend fun syncDB(): SyncState
    fun beginIndex()
    fun addRecord(record: RecordModel)
    suspend fun finishIndex()
    fun close()
}