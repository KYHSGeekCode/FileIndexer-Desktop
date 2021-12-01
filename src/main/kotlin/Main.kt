// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
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
                CommandInput("Index") { path ->
                    // 인덱스를 실행함
                    viewModel.index(path)
                }
                DriveInput(viewModel)
                Row {
                    Button({ viewModel.fetch() }) {
                        // 드라이브에서 다운로드
                        Text("Fetch DB from drive")
                    }
                    Button({ viewModel.upload() }) {
                        //
                        Text("Upload DB to drive")
                    }
                }
                ResultView(viewModel)
            }
        }
    }
}

@Composable
fun DriveInput(viewModel: ViewModel) {
    Column(modifier = Modifier.fillMaxWidth()) {
        val currentProgress = viewModel.currentProgress.collectAsState()
        Row(modifier = Modifier.fillMaxWidth()) {
            val currentAccount = viewModel.currentAccount.collectAsState()
            if (currentAccount.value == null) {
                Button({ viewModel.loginGoogleDrive("user") }) {
                    Text("Login drive")
                }
            } else {
                Button({ viewModel.logoutGoogleDrive() }) {
                    Text("Logout drive")
                }
            }
            Text("Account: ${currentAccount.value}", modifier = Modifier.weight(1.0f).padding(10.dp))
            Button({ viewModel.indexGoogleDrive() }) {
                Text("Index")
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            LinearProgressIndicator(progress = currentProgress.value, modifier = Modifier.weight(1.0f))
            Text("${currentProgress.value * 100}%")
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