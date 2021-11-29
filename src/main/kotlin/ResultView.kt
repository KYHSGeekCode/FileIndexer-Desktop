import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ResultView(viewModel: ViewModel) {
    val uiState = viewModel.uiState.collectAsState().value
    when (uiState) {
        is UiState.Idle -> Text("Nothing in progress")
        is UiState.Find -> FindResult(uiState) // progress, table
        is UiState.Fetch -> {
        } // progress
        is UiState.Upload -> {
        } // progress
        is UiState.Loading -> LoadingView(uiState)
    }
}

@Composable
fun LoadingView(uiState: UiState.Loading) {
    Column {
        Text(uiState.message)
        LinearProgressIndicator(progress = uiState.progress)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FindResult(uiState: UiState.Find) {
    val list = uiState.resultList
    LazyColumn {
        stickyHeader {
            FileResultHeader()
        }
        items(list) { row ->
            FileResultRow(row)
        }
    }
}

@Composable
fun FileResultRow(row: FileRow) {
    Row {
        CellText(row.id.toString(), Modifier.width(100.dp))
        CellText(row.filename, Modifier.width(200.dp))
        CellText(row.tag ?: "", Modifier.width(200.dp))
        CellText(row.major_drive ?: "", Modifier.width(100.dp))
        CellText(row.minor_drive ?: "", Modifier.width(100.dp))
        CellText(row.path ?: "", Modifier.width(500.dp))
    }
}

@Composable
fun FileResultHeader() {
    Row {
        CellText("Id", Modifier.width(100.dp))
        CellText("Filename", Modifier.width(200.dp))
        CellText("Tag", Modifier.width(200.dp))
        CellText("Major Drive", Modifier.width(100.dp))
        CellText("Minor Drive", Modifier.width(100.dp))
        CellText("Path", Modifier.width(500.dp))
    }
}


@Composable
fun CellText(content: String, modifier: Modifier) {
    Box(
        modifier = modifier
            .background(Color.White)
            .border(1.dp, Color.Cyan)
    ) {
        Text(text = content)
    }
}