package live.talkshop.testapp

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import live.talkshop.sdk.core.authentication.TalkShopLive
import live.talkshop.sdk.core.chat.Chat
import live.talkshop.sdk.core.show.Show
import live.talkshop.testapp.ui.theme.TestAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TestAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(this)
                }
            }
        }
    }
}

@Composable
fun MainScreen(context: Context) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        ClientKeyInputSection(context)
        ShowIdInputSection()
        InitializeChat()
        PublishMessage()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientKeyInputSection(context: Context) {
    var clientKey by remember { mutableStateOf("sdk_2ea21de19cc8bc5e8640c7b227fef2f3") }
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
                debugMode = false,
                testMode = false,
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
fun ShowIdInputSection() {
    var showId by remember { mutableStateOf("8WtAFFgRO1K0") }
    var showDetails by remember { mutableStateOf<String?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    OutlinedTextField(
        value = showId,
        onValueChange = { showId = it },
        label = { Text("Show ID") },
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(16.dp))

    Row {
        Button(
            onClick = {
                coroutineScope.launch {
                    Show.getDetails(showId) { error, show ->
                        if (error == null && show != null) {
                            showDetails =
                                "ID: ${show.id}, " +
                                        "\nName: ${show.name}, " +
                                        "\nDescription: ${show.description}, " +
                                        "\nStatus: ${show.status}, " +
                                        "\nTrailer URL: ${show.trailerUrl}, " +
                                        "\nHLS URL: ${show.hlsUrl}, " +
                                        "\nHLS Playback URL: ${show.hlsPlaybackUrl}"
                            errorText = null
                        } else {
                            errorText = error
                            showDetails = null
                        }

                    }
                }
            },
            modifier = Modifier.wrapContentWidth(Alignment.End)
        ) {
            Text("Fetch Show Details")
        }

        Spacer(modifier = Modifier.width(5.dp))

        Button(
            onClick = {
                coroutineScope.launch {
                    Show.getStatus(showId) { error, show ->
                        if (error == null && show != null) {
                            showDetails = "Show Key: ${show.showKey}, " +
                                    "\nShow Status: ${show.status}," +
                                    "\nHLS Playback URL: ${show.hlsPlaybackUrl}," +
                                    "\nHLS URL: ${show.hlsUrl}"
                            errorText = null
                        } else {
                            errorText = error
                            showDetails = null
                        }

                    }
                }
            },
            modifier = Modifier.wrapContentWidth(Alignment.End)
        ) {
            Text("Fetch Show Status")
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
fun InitializeChat() {
    var jwt by remember { mutableStateOf("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzZGtfMmVhMjFkZTE5Y2M4YmM1ZTg2NDBjN2IyMjdmZWYyZjMiLCJleHAiOjE3MTE3NTk1MzQsInVzZXIiOnsiaWQiOiJ0c2wtaW50ZXJuYWwtdGVzdC11c2VyIiwibmFtZSI6IlJvbiBMdWdnZSJ9LCJqdGkiOiJ0V2hCQXdTVG1YVTZ6eVFLMTVFdXl5PT0ifQ.GVizw762W-1yz5Lt8zDlBhUQiNPXd4UuE4r_xlyUqzI") }
    var isGuest by remember { mutableStateOf(false) }
    var showId by remember { mutableStateOf("8WtAFFgRO1K0") }  // Mutable state for showId
    var apiResult by remember { mutableStateOf<String?>(null) }

    OutlinedTextField(
        value = jwt,
        onValueChange = { jwt = it },
        label = { Text("JWT Token") },
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = showId,
        onValueChange = { showId = it },
        label = { Text("Show ID") },
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
            Chat(showId, jwt, isGuest) { errorMessage, userTokenModel ->
                apiResult = errorMessage ?: "Great success! UserId: ${userTokenModel?.userId}"
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublishMessage() {
    var message by remember { mutableStateOf("") }
    var apiResult by remember { mutableStateOf<String?>(null) }

    OutlinedTextField(
        value = message,
        onValueChange = { message = it },
        label = { Text("Message") },
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(16.dp))

    Button(
        onClick = {
            Chat.publish(message) { error, timetoken ->
                apiResult = if (error == null) {
                    "Message sent, timetoken: $timetoken"
                } else {
                    "Failed to send message: $error"
                }
            }
        },
        modifier = Modifier.wrapContentWidth(Alignment.End)
    ) {
        Text("Send Message")
    }

    Spacer(modifier = Modifier.height(16.dp))

    apiResult?.let {
        Text(it, color = if (!it.startsWith("Failed")) Color.Green else Color.Red)
    }
}