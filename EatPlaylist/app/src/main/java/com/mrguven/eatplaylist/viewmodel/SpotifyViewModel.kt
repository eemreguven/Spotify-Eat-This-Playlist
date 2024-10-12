package com.mrguven.eatplaylist.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote

class SpotifyViewModel : ViewModel() {
    private var spotifyAppRemote: SpotifyAppRemote? = null
    private val _currentSongImage = MutableLiveData<Bitmap>()
    val currentSongImage: LiveData<Bitmap> get() = _currentSongImage

    fun connectSpotify(context: Context, clientId: String, redirectUri: String) {
        val connectionParams = ConnectionParams.Builder(clientId)
            .setRedirectUri(redirectUri)
            .showAuthView(true)
            .build()

        SpotifyAppRemote.connect(context, connectionParams, object : Connector.ConnectionListener {
            override fun onConnected(appRemote: SpotifyAppRemote) {
                spotifyAppRemote = appRemote
                resumeCurrentSong()
            }

            override fun onFailure(throwable: Throwable) {
                Log.e("SpotifyViewModel", "Connection failed: ${throwable.message}", throwable)
            }
        })
    }

    fun playPlaylist(playlistId: String) {
        spotifyAppRemote?.let {
            val playlistURI = "spotify:playlist:${playlistId}"
            it.playerApi.play(playlistURI)
            subscribeToPlayerState()
        }
    }

    fun skipToNextTrack() {
        spotifyAppRemote?.playerApi?.skipNext()
    }

    private fun resumeCurrentSong() {
        spotifyAppRemote?.playerApi?.resume()?.setResultCallback {
            subscribeToPlayerState()
        }
    }

    private fun subscribeToPlayerState() {
        spotifyAppRemote?.let { spotifyRemote ->
            spotifyRemote.playerApi.subscribeToPlayerState()
                .setEventCallback { playerApi ->
                    val track = playerApi.track
                    spotifyRemote.imagesApi.getImage(track.imageUri)
                        .setResultCallback { newBitmap ->
                            _currentSongImage.postValue(newBitmap)
                        }
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        spotifyAppRemote?.let {
            SpotifyAppRemote.disconnect(it)
        }
    }
}
