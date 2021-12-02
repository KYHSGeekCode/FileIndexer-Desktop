import com.google.api.client.util.DateTime
import java.time.LocalDateTime

sealed interface SyncState {
    object None : SyncState
    object Loading : SyncState
    class Success(
        val merge: Boolean,
        val upload: Boolean,
        val lastModifiedDrive: DateTime?,
        val lastModifiedLocal: LocalDateTime,
        val date: LocalDateTime
    ) :
        SyncState

    class Fail(val error: String) : SyncState
}