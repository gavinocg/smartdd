package com.smartdd.app.navigation

import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.smartdd.app.NotificationPayload
import com.smartdd.app.presentation.WebSocketViewModel
import com.smartdd.app.presentation.auth.LoginScreen
import com.smartdd.app.presentation.auth.RegisterScreen
import com.smartdd.app.presentation.call.chat.ChatScreen
import com.smartdd.app.presentation.home.HomeScreen
import com.smartdd.app.presentation.profile.ProfileScreen
import com.smartdd.app.presentation.qr.ar.ARDoorbellScreen
import com.smartdd.app.presentation.qr.generate.GenerateQRScreen
import com.smartdd.app.presentation.qr.scanner.ScannerScreen
import com.smartdd.app.presentation.call.audio.AudioCallScreen
import com.smartdd.app.presentation.call.video.VideoCallScreen
import com.smartdd.app.presentation.ring.CallViewModel
import com.smartdd.app.presentation.ring.receiver.IncomingCallScreen
import com.smartdd.app.presentation.ring.sender.WaitingScreen
import com.smartdd.app.presentation.settings.SettingsScreen
import com.smartdd.app.presentation.splash.SplashScreen

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
    const val GENERATE_QR = "generate_qr"
    const val SCANNER = "scanner"
    const val AR_DOORBELL = "ar_doorbell/{qrUuid}"
    const val WAITING = "waiting/{sessionId}/{roomId}"
    const val INCOMING_CALL = "incoming_call/{sessionId}/{roomId}?emisorName={emisorName}"
    const val AUDIO_CALL = "audio_call/{roomId}/{sessionId}?callerName={callerName}"
    const val VIDEO_CALL = "video_call/{roomId}/{sessionId}?callerName={callerName}"
    const val CHAT = "chat/{sessionId}"
    const val PROFILE = "profile"
    const val SETTINGS = "settings"

    fun arDoorbell(qrUuid: String) = "ar_doorbell/$qrUuid"
    fun waiting(sessionId: String, roomId: String) = "waiting/$sessionId/$roomId"
    fun incomingCall(sessionId: String, roomId: String, emisorName: String? = null) =
        "incoming_call/$sessionId/$roomId${emisorName?.let { "?emisorName=$it" } ?: ""}"
    fun audioCall(roomId: String, sessionId: String, callerName: String? = null) =
        "audio_call/$roomId/$sessionId${callerName?.let { "?callerName=$it" } ?: ""}"
    fun videoCall(roomId: String, sessionId: String, callerName: String? = null) =
        "video_call/$roomId/$sessionId${callerName?.let { "?callerName=$it" } ?: ""}"
    fun chat(sessionId: String) = "chat/$sessionId"
}

@Composable
fun SmartDDNavGraph(notificationPayload: NotificationPayload? = null) {
    val navController = rememberNavController()
    val callViewModel: CallViewModel = hiltViewModel()
    val webSocketViewModel: WebSocketViewModel = hiltViewModel()
    val webSocketClient = webSocketViewModel.webSocketClient

    // Deep link desde notificación FCM
    LaunchedEffect(notificationPayload) {
        val payload = notificationPayload ?: return@LaunchedEffect
        when (payload.navigate) {
            "incoming_call" -> {
                val sId = payload.sessionId ?: return@LaunchedEffect
                val rId = payload.roomId ?: return@LaunchedEffect
                navController.navigate(Routes.incomingCall(sId, rId, payload.emisorName)) {
                    popUpTo(Routes.HOME) { inclusive = false }
                }
            }
            "chat" -> {
                val sId = payload.sessionId ?: return@LaunchedEffect
                navController.navigate(Routes.chat(sId)) {
                    popUpTo(Routes.HOME) { inclusive = false }
                }
            }
        }
    }

    NavHost(navController = navController, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) {
            SplashScreen(onNavigateToLogin = {
                navController.navigate(Routes.LOGIN) {
                    popUpTo(Routes.SPLASH) { inclusive = true }
                }
            }, onNavigateToHome = {
                navController.navigate(Routes.HOME) {
                    popUpTo(Routes.SPLASH) { inclusive = true }
                }
            })
        }

        composable(Routes.LOGIN) {
            LoginScreen(onNavigateToRegister = {
                navController.navigate(Routes.REGISTER)
            }, onLoginSuccess = {
                navController.navigate(Routes.HOME) {
                    popUpTo(Routes.LOGIN) { inclusive = true }
                }
            })
        }

        composable(Routes.REGISTER) {
            RegisterScreen(onNavigateToLogin = {
                navController.popBackStack()
            }, onRegisterSuccess = {
                navController.navigate(Routes.HOME) {
                    popUpTo(Routes.REGISTER) { inclusive = true }
                }
            })
        }

        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToGenerate = { navController.navigate(Routes.GENERATE_QR) },
                onNavigateToScanner = { navController.navigate(Routes.SCANNER) },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                onNavigateToProfile = { navController.navigate(Routes.PROFILE) }
            )
        }

        composable(Routes.GENERATE_QR) {
            GenerateQRScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.SCANNER) {
            ScannerScreen(onQrDetected = { uuid ->
                navController.navigate(Routes.arDoorbell(uuid))
            }, onBack = { navController.popBackStack() })
        }

        composable(
            route = Routes.AR_DOORBELL,
            arguments = listOf(navArgument("qrUuid") { type = NavType.StringType })
        ) { backStackEntry ->
            val qrUuid = backStackEntry.arguments?.getString("qrUuid") ?: return@composable
            ARDoorbellScreen(
                qrUuid = qrUuid,
                onRingSent = { sessionId, roomId ->
                    navController.navigate(Routes.waiting(sessionId, roomId)) {
                        popUpTo(Routes.AR_DOORBELL) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.WAITING,
            arguments = listOf(
                navArgument("sessionId") { type = NavType.StringType },
                navArgument("roomId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: return@composable
            val roomId = backStackEntry.arguments?.getString("roomId") ?: return@composable
            WaitingScreen(
                sessionId = sessionId,
                roomId = roomId,
                webSocketClient = webSocketClient,
                onCancel = { navController.popBackStack(Routes.HOME, false) },
                onNavigateToChat = { sId ->
                    navController.navigate(Routes.chat(sId)) {
                        popUpTo(Routes.HOME) { inclusive = false }
                    }
                },
                onNavigateToAudioCall = { rId, sId ->
                    navController.navigate(Routes.audioCall(rId, sId)) {
                        popUpTo(Routes.HOME) { inclusive = false }
                    }
                },
                onNavigateToVideoCall = { rId, sId ->
                    navController.navigate(Routes.videoCall(rId, sId)) {
                        popUpTo(Routes.HOME) { inclusive = false }
                    }
                }
            )
        }

        composable(
            route = Routes.INCOMING_CALL,
            arguments = listOf(
                navArgument("sessionId") { type = NavType.StringType },
                navArgument("roomId") { type = NavType.StringType },
                navArgument("emisorName") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: return@composable
            val roomId = backStackEntry.arguments?.getString("roomId") ?: return@composable
            val emisorName = backStackEntry.arguments?.getString("emisorName")
            IncomingCallScreen(
                sessionId = sessionId,
                roomId = roomId,
                emisorName = emisorName,
                onRespond = { action, mode ->
                    callViewModel.respond(sessionId, action, mode)
                },
                onEndCall = { navController.popBackStack(Routes.HOME, false) },
                onNavigateToChat = { sId -> navController.navigate(Routes.chat(sId)) },
                onNavigateToAudioCall = { rId, sId ->
                    navController.navigate(Routes.audioCall(rId, sId, emisorName)) {
                        popUpTo(Routes.HOME) { inclusive = false }
                    }
                },
                onNavigateToVideoCall = { rId, sId ->
                    navController.navigate(Routes.videoCall(rId, sId, emisorName)) {
                        popUpTo(Routes.HOME) { inclusive = false }
                    }
                }
            )
        }

        composable(
            route = Routes.AUDIO_CALL,
            arguments = listOf(
                navArgument("roomId") { type = NavType.StringType },
                navArgument("sessionId") { type = NavType.StringType },
                navArgument("callerName") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) { backStackEntry ->
            val roomId = backStackEntry.arguments?.getString("roomId") ?: return@composable
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: return@composable
            val callerName = backStackEntry.arguments?.getString("callerName")
            AudioCallScreen(
                roomId = roomId,
                sessionId = sessionId,
                callerName = callerName,
                onEndCall = { navController.popBackStack(Routes.HOME, false) }
            )
        }

        composable(
            route = Routes.VIDEO_CALL,
            arguments = listOf(
                navArgument("roomId") { type = NavType.StringType },
                navArgument("sessionId") { type = NavType.StringType },
                navArgument("callerName") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) { backStackEntry ->
            val roomId = backStackEntry.arguments?.getString("roomId") ?: return@composable
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: return@composable
            VideoCallScreen(
                roomId = roomId,
                sessionId = sessionId,
                onEndCall = { navController.popBackStack(Routes.HOME, false) }
            )
        }

        composable(
            route = Routes.CHAT,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: return@composable
            ChatScreen(
                sessionId = sessionId,
                onBack = { navController.popBackStack(Routes.HOME, false) }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.PROFILE) {
            ProfileScreen(onBack = { navController.popBackStack() })
        }
    }
}
