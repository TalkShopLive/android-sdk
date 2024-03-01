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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import live.talkshop.sdk.core.authentication.TalkShopLive
import live.talkshop.sdk.core.show.Show
import live.talkshop.sdk.core.show.models.ShowModel
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
        CreateUserInputSection(context)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientKeyInputSection(context: Context) {
    var clientKey by remember { mutableStateOf("") }
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
    var showId by remember { mutableStateOf("") }
    var showDetails by remember { mutableStateOf<ShowModel?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }

    OutlinedTextField(
        value = showId,
        onValueChange = { showId = it },
        label = { Text("Show ID") },
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(16.dp))

    Button(
        onClick = {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    Show.getDetails(showId, object : Show.GetDetailsCallback {
                        override fun onSuccess(showModel: ShowModel) {
                            showDetails = showModel
                            errorText = null
                        }

                        override fun onError(error: String) {
                            errorText = error
                            showDetails = null
                        }
                    })
                } catch (e: Exception) {
                    errorText = e.message ?: "Unknown error occurred"
                    showDetails = null
                }
            }
        },
        modifier = Modifier.wrapContentWidth(Alignment.End)
    ) {
        Text("Fetch Show Details")
    }

    Spacer(modifier = Modifier.height(8.dp))

    showDetails?.let { ShowDetails(it) }
    errorText?.let { Text(it, color = Color.Red) }
}

@Composable
fun ShowDetails(showModel: ShowModel) {
    Column {
        Text("id: ${showModel.id}")
        Text("Name: ${showModel.name}")
        Text("Description: ${showModel.description}")
        Text("Status: ${showModel.status}")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateUserInputSection(context: Context) {
    var jwt by remember { mutableStateOf("") }
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
            TalkShopLive.Chat(context, jwt, isGuest) { errorMessage, userTokenModel ->
                apiResult = errorMessage ?: "Great success! UserId: ${userTokenModel?.userId}"
            }
        },
        modifier = Modifier.wrapContentWidth(Alignment.End)
    ) {
        Text("Create User")
    }

    Spacer(modifier = Modifier.height(16.dp))

    apiResult?.let {
        Text(it, color = if (it.startsWith("Great success")) Color.Green else Color.Red)
    }
}