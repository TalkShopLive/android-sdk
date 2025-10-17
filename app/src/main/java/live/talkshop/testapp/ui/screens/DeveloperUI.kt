package live.talkshop.testapp.ui.screens

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import live.talkshop.sdk.core.authentication.TalkShopLive
import live.talkshop.sdk.core.chat.Chat
import live.talkshop.sdk.core.chat.ChatCallback
import live.talkshop.sdk.core.chat.models.MessageModel
import live.talkshop.sdk.core.show.Show
import live.talkshop.sdk.core.show.models.ShowModel
import live.talkshop.sdk.resources.APIClientError
import live.talkshop.sdk.resources.CollectorActions

@Composable
fun DeveloperScreen(context: Context, clientKeyString: String, globalShowKey: String, jwt: String) {
    val scrollState = rememberScrollState()
    val timeTokenState = remember { mutableStateOf("17126936597253584") }
    var showModel by remember { mutableStateOf<ShowModel?>(null) }
    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        ClientKeyInputSection(context, clientKeyString)
        ShowIdInputSection(globalShowKey, onShowRetrieved = { showModel = it })
        InitializeChat(jwt, globalShowKey)
        PublishMessage(timeTokenState)
        ChatHistory()
        DeleteMessageSection(timeTokenState)
        showModel?.let { CollectActionSection(it) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientKeyInputSection(context: Context, clientKeyString: String) {
    var clientKey by remember { mutableStateOf(clientKeyString) }
    var initializationResult by remember { mutableStateOf<String?>(null) }

    OutlinedTextField(
        value = clientKey,
        onValueChange = { clientKey = it },
        label = { Text("Client Key") },
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(16.dp))

    Button(
        onClick = {
            TalkShopLive.initialize(
                context,
                clientKey,
                debugMode = true,
                testMode = true,
                dnt = false
            ) {
                initializationResult = if (it) {
                    "Success"
                } else {
                    "Fail"
                }
            }
        },
        modifier = Modifier.wrapContentWidth(Alignment.End)

    ) {
        Text("Initialize SDK")
    }

    Spacer(modifier = Modifier.height(8.dp))

    initializationResult?.let {
        Text(it, color = if (it == "Success") Color.Green else Color.Red)
    }

    Spacer(modifier = Modifier.height(16.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowIdInputSection(globalShowKey: String, onShowRetrieved: (ShowModel) -> Unit) {
    var showKey by remember { mutableStateOf(globalShowKey) }
    var showDetails by remember { mutableStateOf<String?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    OutlinedTextField(
        value = showKey,
        onValueChange = { showKey = it },
        label = { Text("Show ID") },
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(16.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = {
                coroutineScope.launch {
                    Show.getDetails(showKey) { error, show ->
                        if (error == null && show != null) {
                            showDetails = "ID: ${show.id}, " +
                                    "\nName: ${show.name}, " +
                                    "\nDescription: ${show.showDescription}, " +
                                    "\nStatus: ${show.status}, " +
                                    "\nTrailer URL: ${show.trailerUrl}, " +
                                    "\nHLS URL: ${show.hlsUrl}, " +
                                    "\nAir Date: ${show.airDate}, " +
                                    "\nHLS Playback URL: ${show.hlsPlaybackUrl}" +
                                    "\nCC URL: ${show.cc}"
                            errorText = null
                            onShowRetrieved(show) // Pass ShowModel to DeveloperScreen
                        } else {
                            errorText = error.toString()
                            showDetails = null
                        }
                    }
                }
            },
            modifier = Modifier.weight(1f)
        ) {
            Text("Details")
        }

        Button(
            onClick = {
                coroutineScope.launch {
                    Show.getStatus(showKey) { error, show ->
                        if (error == null && show != null) {
                            showDetails = "Show Key: ${show.showKey}, " +
                                    "\nShow Status: ${show.status}," +
                                    "\nDuration: ${show.duration}," +
                                    "\nHLS Playback URL: ${show.hlsPlaybackUrl}," +
                                    "\nHLS URL: ${show.hlsUrl}"
                            errorText = null
                        } else {
                            errorText = error.toString()
                            showDetails = null
                        }
                    }
                }
            },
            modifier = Modifier.weight(1f)
        ) {
            Text("Status")
        }

        Button(
            onClick = {
                coroutineScope.launch {
                    Show.getProducts(showKey) { error, show ->
                        if (error == null && show != null) {
                            showDetails = show.toString()
                            errorText = null
                        } else {
                            errorText = error.toString()
                            showDetails = null
                        }
                    }
                }
            },
            modifier = Modifier.weight(1f)
        ) {
            Text("Products")
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    showDetails?.let { ShowDetails(it) }
    errorText?.let { Text(it, color = Color.Red) }
}


@Composable
fun ShowDetails(showDetails: String) {
    Column {
        Text(showDetails)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InitializeChat(jwtString: String, globalShowKey: String) {
    var jwt by remember { mutableStateOf(jwtString) }
    var isGuest by remember { mutableStateOf(false) }
    var apiResult by remember { mutableStateOf<String?>(null) }

    OutlinedTextField(
        value = jwt,
        onValueChange = { jwt = it },
        label = { Text("JWT Token") },
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(16.dp))

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Is Guest", modifier = Modifier.weight(1f))
        Switch(
            checked = isGuest,
            onCheckedChange = { isGuest = it }
        )
    }

    Button(
        onClick = {
            apiResult = null
            Chat(globalShowKey, jwt, isGuest) { error, userTokenModel ->
                apiResult = error?.toString() ?: "Great success! UserId: ${userTokenModel?.userId}"
            }
        },
        modifier = Modifier.wrapContentWidth(Alignment.End)
    ) {
        Text("Initialize Chat")
    }

    Spacer(modifier = Modifier.height(16.dp))

    apiResult?.let {
        Text(it, color = if (it.startsWith("Great success")) Color.Green else Color.Red)
    }
}

@OptIn(DelicateCoroutinesApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PublishMessage(timeTokenState: MutableState<String>) {
    var message by remember { mutableStateOf("") }
    var apiResult by remember { mutableStateOf<String?>(null) }
    var subscriptionResult by remember { mutableStateOf("") }

    OutlinedTextField(
        value = message,
        onValueChange = { message = it },
        label = { Text("Message") },
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(16.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Button(
            onClick = {
                GlobalScope.launch {
                    Chat.subscribe(object : ChatCallback {
                        override fun onMessageReceived(message: MessageModel) {
                            subscriptionResult =
                                "${message.sender?.name}: Received message: ${message.text}"
                        }

                        override fun onMessageDeleted(messageId: Long) {
                            subscriptionResult = "Deleted message: $messageId"
                        }

                        override fun onStatusChange(error: APIClientError) {
                            Log.e("MainActivity.kt", error.toString())
                            subscriptionResult = "Error: $error"
                        }

                        override fun onLikeComment(messageId: Long) {
                            subscriptionResult = "onLikeComment: $messageId"
                        }

                        override fun onUnlikeComment(messageId: Long) {
                            subscriptionResult = "onUnlikeComment: $messageId"
                        }
                    })
                }
            }
        ) {
            Text("Subscribe")
        }
        Spacer(modifier = Modifier.width(5.dp))
        Button(
            onClick = {
                GlobalScope.launch {
                    Chat.publish(message) { error, timetoken ->
                        apiResult = if (error == null) {
                            timeTokenState.value = timetoken ?: timeTokenState.value
                            "Message sent, timetoken: $timetoken"
                        } else {
                            "Failed to send message: $error"
                        }
                    }
                }
            }
        ) {
            Text("Send Message")
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    apiResult?.let {
        Text(it, color = if (!it.startsWith("Failed")) Color.Green else Color.Red)
    }

    if (subscriptionResult.isNotEmpty()) {
        Text(subscriptionResult, color = Color.Blue)
    }
}

@OptIn(DelicateCoroutinesApi::class)
@Composable
fun ChatHistory() {
    var messages by remember { mutableStateOf<List<MessageModel>?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showPopup by remember { mutableStateOf(false) }
    var chatTimeToken by remember { mutableStateOf<Long?>(null) }

    val context = LocalContext.current

    Button(onClick = {
        GlobalScope.launch {
            Chat.getChatMessages(count = 2) { messageList, newChatTimeToken, error ->
                if (error == null) {
                    messages = messageList
                    chatTimeToken = newChatTimeToken
                } else {
                    errorMessage = error.toString()
                }
                showPopup = true
            }
        }
    }) {
        Text("Get Chat History")
    }

    if (showPopup) {
        AlertDialog(
            onDismissRequest = { showPopup = false },
            title = { Text("Chat History") },
            text = {
                if (messages != null) {
                    LazyColumn {
                        items(messages!!) { message ->
                            message.text?.let { Text(message.sender?.name + ": " + it) }
                        }
                    }
                } else {
                    Text(errorMessage ?: "Unknown error")
                }
            },
            confirmButton = {
                Button(onClick = { showPopup = false }) {
                    Text("OK")
                }
            },
            dismissButton = {
                Button(onClick = {
                    if (chatTimeToken != null) {
                        GlobalScope.launch {
                            Chat.getChatMessages(
                                count = 100,
                                start = chatTimeToken
                            ) { messageList, newChatTimeToken, error ->
                                if (error == null) {
                                    messages = messageList
                                    chatTimeToken = newChatTimeToken
                                } else {
                                    errorMessage = error.toString()
                                }
                            }
                        }
                    } else {
                        Toast.makeText(
                            context,
                            "You've reached end of chat",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }) {
                    Text("Next")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, DelicateCoroutinesApi::class)
@Composable
fun DeleteMessageSection(timeTokenState: MutableState<String>) {
    var results by remember { mutableStateOf<String?>(null) }

    OutlinedTextField(
        value = timeTokenState.value,
        onValueChange = { timeTokenState.value = it },
        label = { Text("Time Token") },
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(16.dp))

    Button(
        onClick = {
            GlobalScope.launch {
                Chat.deleteMessage(timeTokenState.value) { success, errorMessage ->
                    results = if (success) {
                        "Success"
                    } else {
                        errorMessage
                    }
                }
            }
        },
        modifier = Modifier.wrapContentWidth(Alignment.End)

    ) {
        Text("Delete Message")
    }

    Spacer(modifier = Modifier.height(8.dp))

    results?.let {
        Text(it, color = if (it == "Success") Color.Green else Color.Red)
    }

    Spacer(modifier = Modifier.height(16.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectActionSection(show: ShowModel) {
    var selectedAction by remember { mutableStateOf<CollectorActions?>(null) }
    val collectInstance = remember { TalkShopLive.Collect(show) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        var expanded by remember { mutableStateOf(false) }

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = selectedAction?.name ?: "Select an Action",
                onValueChange = {},
                readOnly = true,
                label = { Text("Collector Action") },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                CollectorActions.values().forEach { action ->
                    DropdownMenuItem(
                        text = { Text(action.name.replace("_", " ")) },
                        onClick = {
                            selectedAction = action
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                selectedAction?.let { action ->
                    collectInstance.collect(eventName = action, videoTime = 1)
                }
            },
            modifier = Modifier.align(Alignment.End),
            enabled = selectedAction != null
        ) {
            Text("Collect")
        }
    }
}