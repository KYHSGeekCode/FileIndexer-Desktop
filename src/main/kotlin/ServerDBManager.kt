import com.google.api.services.drive.Drive

class ServerDBManager : DBManager {
    override fun find(query: String): List<FileRow> {
        TODO("Not yet implemented")
    }

    override fun syncDB(drive: Drive): SyncState {
        TODO("Not yet implemented")
    }

    override fun beginIndex() {
        TODO("Not yet implemented")
    }

    override fun addRecord(record: RecordModel) {
        TODO("Not yet implemented")
    }

    override fun finishIndex() {
        TODO("Not yet implemented")
    }

    override fun close() {
        TODO("Not yet implemented")
    }

}