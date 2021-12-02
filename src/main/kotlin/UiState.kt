sealed interface UiState {
    object Idle : UiState // 아무 정보도 없음
    class Loading(val progress: Float = 0.0f, val message: String = "Loading") : UiState // Loading
    class Find(val query: String, val resultList: List<FileRow>) : UiState // 쿼리
    class Error(val message: String) : UiState
}