// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    val viewModel = ViewModel()
    Window(onCloseRequest = { viewModel.close() }, title = "File Indexer") {
        MaterialTheme {
            Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
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
}


@Composable
fun CommandInput(name: String, default: String = "", onRun: (String) -> Unit = {}) {
    var text by remember { mutableStateOf(default) }
    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        TextField(
            value = text, { text = it },
            modifier = Modifier.fillMaxHeight().weight(1.0f).background(Color.White)
        )
        Button(onClick = {
            onRun(text)
        }, modifier = Modifier.fillMaxHeight().width(100.dp)) {
            Text(name)
        }
    }
}