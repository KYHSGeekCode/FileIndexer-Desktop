// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@ExperimentalComposeUiApi
fun main() = application {
    val coroutineScope = rememberCoroutineScope()
    val viewModel = remember { ViewModel(coroutineScope, SQLiteDBManager()) }
    coroutineScope.launch(Dispatchers.IO) {
        viewModel.silentLogin()?.join()
        viewModel.trySyncDB()
    }
    Window(onCloseRequest = { viewModel.close() }, title = "File Indexer") {
        MaterialTheme {
            Column(
                modifier = Modifier.fillMaxWidth().padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FindInput(viewModel)
                IndexInput(viewModel)
                DriveInput(viewModel)
                SyncRow(viewModel)
                ResultView(viewModel)
            }
        }
    }
}

@Composable
fun SyncRow(viewModel: ViewModel) {
    val syncStatus = viewModel.syncState.collectAsState().value
    Row(
        modifier = groupBoxModifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button({ viewModel.trySyncDB() }) {
            Text("Sync DB with drive")
        }
        when (syncStatus) {
            is SyncState.Fail -> Text("Sync status: Failed (${syncStatus.error})")
            SyncState.None -> Text("Sync status: None")
            is SyncState.Success -> Text(
                "Latest drive date: ${syncStatus.lastModifiedDrive}, " +
                        "Latest local date: ${syncStatus.lastModifiedLocal}, " +
                        "merged: ${syncStatus.merge}, uploaded: ${syncStatus.upload}, " +
                        "Latest sync date: ${syncStatus.date}"
            )
            SyncState.Loading -> Text("In progress")
        }
    }
}

@ExperimentalComposeUiApi
@Composable
fun FindInput(viewModel: ViewModel) {
    var query by remember { mutableStateOf("") }
    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        FancyTextField(
            value = query,
            label = "Query",
            icon = Icons.Filled.Delete,
            modifier = Modifier.fillMaxHeight().weight(1.0f),
            onValueChange = { query = it },
            onIconClick = { query = "" }
        ) {
            viewModel.find(query)
        }
        Button({
            viewModel.find(query)
        }, modifier = Modifier.fillMaxHeight()) {
            Text("Find")
        }
    }
}

@ExperimentalComposeUiApi
@Composable
fun IndexInput(viewModel: ViewModel) {
    var computerName by remember { mutableStateOf(getLocalDeviceName()) }
    var rootPath by remember { mutableStateOf("") }
    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        FancyTextField(
            value = computerName,
            label = "Computer name",
            icon = Icons.Filled.Refresh,
            modifier = Modifier.fillMaxHeight().weight(0.7f),
            onValueChange = { computerName = it },
            onIconClick = { computerName = getLocalDeviceName() }
        )
        FancyTextField(
            value = rootPath,
            label = "Root path",
            icon = Icons.Filled.Delete,
            modifier = Modifier.fillMaxHeight().weight(1.0f),
            onValueChange = { rootPath = it },
            onIconClick = { rootPath = "" }
        )

        Button({
            viewModel.index(computerName, rootPath)
        }, modifier = Modifier.fillMaxHeight()) {
            Text("Index")
        }
    }
}

@Composable
fun DriveInput(viewModel: ViewModel) {
    Column(modifier = groupBoxModifier) {
        val currentProgress = viewModel.currentProgress.collectAsState()
        Row(modifier = Modifier.fillMaxWidth()) {
            val currentAccount = viewModel.currentAccount.collectAsState()
            if (currentAccount.value == null) {
                Button({ viewModel.loginGoogleDrive() }) {
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
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            LinearProgressIndicator(progress = currentProgress.value, modifier = Modifier.weight(1.0f))
            Text("${currentProgress.value * 100}%")
        }
    }
}

@ExperimentalComposeUiApi
@Composable
fun FancyTextField(
    value: String,
    label: String,
    icon: ImageVector,
    modifier: Modifier,
    onValueChange: (String) -> Unit,
    onIconClick: () -> Unit,
    onEnterPress: () -> Unit = {}
) {
    TextField(
        value = value, { onValueChange(it) },
        label = {
            Text(label)
        },
        shape = RoundedCornerShape(8.dp),
        trailingIcon = {
            Icon(icon, "", tint = MaterialTheme.colors.primary, modifier = Modifier.clickable {
                onIconClick()
            })
        },

        modifier = modifier.fillMaxHeight().background(Color.White).padding(4.dp).onKeyEvent {
            if (it.key.nativeKeyCode == Key.Enter.nativeKeyCode) {
                onEnterPress()
                return@onKeyEvent true
            }
            return@onKeyEvent false
        },
        colors = TextFieldDefaults.textFieldColors(
            backgroundColor = Color.White,
            focusedIndicatorColor = Color.Cyan, //hide the indicator
            unfocusedIndicatorColor = MaterialTheme.colors.secondary
        )
    )
}

val groupBoxModifier = Modifier.fillMaxWidth().border(2.dp, Color.LightGray, RoundedCornerShape(4.dp)).padding(10.dp)