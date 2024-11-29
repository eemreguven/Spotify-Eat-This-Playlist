package com.mrguven.eatplaylist

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mrguven.eatplaylist.ui.game.GameScreen
import com.mrguven.eatplaylist.ui.theme.EatPlaylistTheme
import com.mrguven.eatplaylist.ui.game.GameViewModel
import com.mrguven.eatplaylist.ui.spotify.SpotifyViewModel
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse
import com.spotify.sdk.android.auth.app.SpotifyAuthHandler
import kotlin.math.floor

class MainActivity : ComponentActivity() {
    private val clientId = BuildConfig.SPOTIFY_CLIENT_ID
    private val redirectUri = BuildConfig.REDIRECT_URI
    private val requestCode = 1337

    private lateinit var authResultLauncher: ActivityResultLauncher<Intent>

    private val gameViewModel by viewModels<GameViewModel>()
    private val spotifyViewModel by viewModels<SpotifyViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupAuthResultLauncher()
        authenticateWithSpotify()

        enableEdgeToEdge()
        setContent {
            EatPlaylistTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    InitScreenDetails()
                    GameScreen(
                        modifier = Modifier.padding(innerPadding),
                        viewModel = gameViewModel
                    ) {
                        spotifyViewModel.skipToNextTrack()
                        spotifyViewModel.currentSongImage.observe(this) { bitmap ->
                            gameViewModel.generateNewTarget(bitmap)
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        spotifyViewModel.connectSpotify(this, clientId, redirectUri)
    }

    private fun setupAuthResultLauncher() {
        authResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    val data = result.data
                    val response = AuthorizationClient.getResponse(result.resultCode, data)
                    when (response.type) {
                        AuthorizationResponse.Type.TOKEN -> {
                            val accessToken = response.accessToken
                            // Use this token to access Spotify API
                        }

                        AuthorizationResponse.Type.ERROR -> {
                            // Handle error
                            val error = response.error
                        }

                        else -> {
                            // Handle other response types if needed
                        }
                    }
                } else {
                    // Handle other result codes (e.g., Activity.RESULT_CANCELED)
                }
            }
    }

    private fun authenticateWithSpotify() {
        SpotifyAuthHandler()
        val request =
            AuthorizationRequest.Builder(clientId, AuthorizationResponse.Type.TOKEN, redirectUri)
                .setScopes(arrayOf("user-read-private", "playlist-read-private"))
                .build()
        AuthorizationClient.openLoginActivity(this, requestCode, request)
    }

    @Composable
    private fun InitScreenDetails() {
        val configuration = LocalConfiguration.current
        val screenWidthDp = configuration.screenWidthDp.dp
        val screenHeightDp = configuration.screenHeightDp.dp
        val cellSize = 32.dp
        val columns = floor(screenWidthDp / cellSize).toInt()
        val rows = floor(screenHeightDp / cellSize).toInt()
        val cellSizePx = dpToPx(dp = cellSize)
        gameViewModel.initGridData(rows, columns, cellSizePx)
    }

    @Composable
    fun dpToPx(dp: Dp): Float {
        return with(LocalDensity.current) { dp.toPx() }
    }
}
