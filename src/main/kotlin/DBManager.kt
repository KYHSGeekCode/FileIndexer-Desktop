import com.google.api.services.drive.Drive

interface DBManager {
    fun find(query: String): List<FileRow>
    fun syncDB(drive: Drive): SyncState
    fun beginIndex()
    fun addRecord(record: RecordModel)
    fun finishIndex()
    fun close()
}