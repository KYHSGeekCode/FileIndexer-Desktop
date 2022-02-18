import com.google.api.services.drive.Drive


class FirestoreDBManager : DBManager {
    val recordsToAdd = mutableListOf<RecordModel>()


    override fun find(query: String): List<FileRow> {
        TODO()
    }

    override fun syncDB(drive: Drive): SyncState {
        TODO("Not yet implemented")
    }

    override fun beginIndex() {
        TODO("Not yet implemented")
    }

    override fun addRecord(record: RecordModel) {
        recordsToAdd.add(record)
    }

    override fun finishIndex() {

    }

    override fun close() {
        TODO("Not yet implemented")
    }
}