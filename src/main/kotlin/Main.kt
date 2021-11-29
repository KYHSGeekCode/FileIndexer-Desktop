// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
import androidx.compose.desktop.Window
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*

fun main() {
    val viewModel = ViewModel()
    Window(title = "File Indexer") {
        MaterialTheme {
            Column {
                CommandInput("Find") { query ->
                    // sqlite db에서 읽어서 잘 보여줌
                    viewModel.find(query)
                }
                CommandInput("Fetch") { _ ->
                    // 드라이브에서 다운로드
                    viewModel.fetch()
                }
                CommandInput("Index") { path ->
                    // 인덱스를 실행함
                    viewModel.index(path)
                }
                CommandInput("Upload") {
                    //
                    viewModel.upload()
                }
                ResultView(viewModel)
            }
        }
    }
//    viewModel.close()
}


@Composable
fun CommandInput(name: String, default: String = "", onRun: (String) -> Unit = {}) {
    var text by remember { mutableStateOf(default) }
    Row {
        TextField(value = text, { text = it })
        Button(onClick = {
            onRun(text)
        }) {
            Text(name)
        }
    }
}