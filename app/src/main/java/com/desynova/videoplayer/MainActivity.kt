package com.desynova.videoplayer

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.desynova.videoplayer.databinding.ActivityMainBinding
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.util.Util

/**
 * A fullscreen activity to play audio or video streams.
 */
class MainActivity : AppCompatActivity(), Player.Listener {

    private val viewBinding by lazy(LazyThreadSafetyMode.NONE) {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private var player: SimpleExoPlayer? = null

    // Because playWhenReady is initially true,
    // playback starts automatically the first time the app is run.
    private var playWhenReady = true

    // Both currentWindow and playbackPosition are initialized to zero
    // so that playback starts from the very start the first time the app is run.
    private var currentWindow = 0
    private var playbackPosition = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)
    }

    private fun initializePlayer() {
        // Responsible for choosing tracks in the media item to vary the quality of the stream
        // based on the available network bandwidth.
        // This allows the player to quickly switch between tracks as available bandwidth changes.
        val trackSelector = DefaultTrackSelector(this).apply {
            // Tell trackSelector to only pick tracks of standard definition (Sd) or lower —
            // a good way of saving user's data at the expense of quality.
            setParameters(buildUponParameters().setMaxVideoSizeSd())
        }

        player = SimpleExoPlayer.Builder(this)
            .setTrackSelector(trackSelector)
            .build()
            .also { exoPlayer ->
                viewBinding.videoView.player = exoPlayer
                val mediaItem = MediaItem.fromUri(getString(R.string.media_url))
                exoPlayer.setMediaItem(mediaItem)

                // Tells the player whether to start playing
                // as soon as all resources for playback have been acquired.
                exoPlayer.playWhenReady = playWhenReady

                // Tells the player to seek to a certain position within a specific window.
                exoPlayer.seekTo(currentWindow, playbackPosition)

                // Register playbackStateListener with the player.
                exoPlayer.addListener(this)

                exoPlayer.prepare()
            }
    }

    override fun onStart() {
        super.onStart()

        // Android API level 24 and higher supports multiple windows.
        // As app can be visible, but not active in split window mode,
        // we need to initialize the player in onStart.
        if (Util.SDK_INT >= 24) {
            initializePlayer()
        }
    }

    override fun onStop() {
        super.onStop()

        // With API Level 24 and higher (which brought multi- and split-window mode),
        // onStop is guaranteed to be called.
        // In the paused state, activity is still visible,
        // so we wait to release the player until onStop.
        if (Util.SDK_INT >= 24) {
            releasePlayer()
        }
    }

    override fun onResume() {
        super.onResume()

        hideSystemUi()

        // Android API level 24 and lower requires to wait
        // as long as possible until resources are grabbed,
        // so we wait until onResume before initializing the player.
        if ((Util.SDK_INT < 24 || player == null)) {
            initializePlayer()
        }
    }

    override fun onPause() {
        super.onPause()

        // With API Level 24 and lower, there is no guarantee of onStop being called,
        // so we release the player as early as possible in onPause.
        if (Util.SDK_INT < 24) {
            releasePlayer()
        }
    }

    // Allows to have a full screen experience
    private fun hideSystemUi() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, viewBinding.root).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.hide(WindowInsetsCompat.Type.navigationBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun releasePlayer() {
        player?.run {
            // Allows to resume playback from where the user left off
            playbackPosition = this.currentPosition
            currentWindow = this.currentWindowIndex
            this@MainActivity.playWhenReady = this.playWhenReady

            // Deregister to avoid dangling references from the player
            // which could cause a memory leak.
            removeListener(this@MainActivity)

            release()
        }
        player = null
    }

    private fun showProgressBar() {
        viewBinding.progressBar.visibility = View.VISIBLE
    }

    private fun hideProgressBar() {
        viewBinding.progressBar.visibility = View.GONE
    }

    override fun onPlaybackStateChanged(state: Int) {
        when (state) {
            ExoPlayer.STATE_BUFFERING -> {
                showProgressBar()
            }
            ExoPlayer.STATE_READY -> {
                hideProgressBar()
            }
            ExoPlayer.STATE_ENDED -> {
                hideProgressBar()
            }
            ExoPlayer.STATE_IDLE -> {
                hideProgressBar()
            }
        }
    }
}