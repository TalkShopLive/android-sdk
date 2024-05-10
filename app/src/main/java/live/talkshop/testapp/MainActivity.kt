package live.talkshop.testapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import live.talkshop.testapp.ui.screens.DeveloperScreen
import live.talkshop.testapp.ui.screens.LoginScreen
import live.talkshop.testapp.ui.theme.TestAppTheme

//Update these values to test scenarios
private const val clientKey = ""
private const val globalShowKey = ""
private const val jwt = ""

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TestAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavHost(navController, startDestination = "login_screen") {
                        composable("login_screen") {
                            LoginScreen(
                                context = this@MainActivity,
                                clientKeyString = clientKey,
                                showKeyString = globalShowKey,
                                onLogoClick = {
                                    navController.navigate("developer_screen")
                                },
                                onWatchShowClick = { clientKey, showKey ->
                                    println("Client Key: $clientKey, Show Key: $showKey")
                                }
                            )
                        }

                        composable("developer_screen") {
                            DeveloperScreen(
                                context = this@MainActivity,
                                clientKeyString = clientKey,
                                globalShowKey = globalShowKey,
                                jwt = jwt
                            )
                        }
                    }
                }
            }
        }
    }
}
