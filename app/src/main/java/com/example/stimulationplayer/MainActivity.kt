package com.example.stimulationplayer

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.stimulationplayer.data.ScriptValidator
import com.example.stimulationplayer.data.TimingModel
import com.example.stimulationplayer.data.ValidationException
import com.example.stimulationplayer.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var videoUri: Uri? = null
    private var scriptUri: Uri? = null
    private var script: com.example.stimulationplayer.data.Script? = null
    private var timingModel: com.example.stimulationplayer.data.TimingModel? = null
    private var player: androidx.media3.common.Player? = null
    private lateinit var audioEngine: AudioEngine
    private var syncEngine: SyncEngine? = null
    private var isFullscreen = false
    private lateinit var gestureDetector: GestureDetector
    private var areSoundsLoaded = false

    companion object {
        private const val KEY_VIDEO_URI = "video_uri"
        private const val KEY_SCRIPT_URI = "script_uri"
    }


    private val selectVideoLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.also { uri ->
                    contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    videoUri = uri
                    binding.videoFileName.text = getFileName(uri)
                    checkFilesAndEnablePlay()
                }
            }
        }

    private val selectScriptLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.also { uri ->
                    contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    scriptUri = uri
                    binding.scriptFileName.text = getFileName(uri)
                    checkFilesAndEnablePlay()
                }
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        savedInstanceState?.let { bundle ->
            videoUri = bundle.getParcelable(KEY_VIDEO_URI)
            scriptUri = bundle.getParcelable(KEY_SCRIPT_URI)

            videoUri?.let { binding.videoFileName.text = getFileName(it) }
            scriptUri?.let { binding.scriptFileName.text = getFileName(it) }

            checkFilesAndEnablePlay()
        }

        binding.selectVideoButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "video/*"
            }
            selectVideoLauncher.launch(intent)
        }

        binding.selectScriptButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/json"
            }
            selectScriptLauncher.launch(intent)
        }

        binding.playButton.setOnClickListener {
            play()
        }

        binding.stopButton.setOnClickListener {
            stop()
        }

        initializePlayer()
        audioEngine = AudioEngine(this) {
            areSoundsLoaded = true
            checkFilesAndEnablePlay()
        }
        setupGestureDetector()
    }

    private fun stop() {
        player?.stop()
        player?.clearMediaItems()
        binding.stopButton.isEnabled = false
        syncEngine?.stop()
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
        audioEngine.release()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        videoUri?.let { outState.putParcelable(KEY_VIDEO_URI, it) }
        scriptUri?.let { outState.putParcelable(KEY_SCRIPT_URI, it) }
    }

    private fun initializePlayer() {
        player = androidx.media3.exoplayer.ExoPlayer.Builder(this).build()
        binding.playerView.player = player
    }

    private fun releasePlayer() {
        player?.release()
        player = null
    }

    private fun play() {
        if (player != null && videoUri != null && script != null && timingModel != null) {
            syncEngine = SyncEngine(player!!, timingModel!!, audioEngine) {
                binding.overlayText.text = it
                binding.overlayText.visibility = if (it.isNotEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            }

            val mediaItem = androidx.media3.common.MediaItem.fromUri(videoUri!!)
            player?.setMediaItem(mediaItem)
            player?.prepare()
            player?.play()
            binding.stopButton.isEnabled = true
            syncEngine?.start()
        }
    }


    private fun checkFilesAndEnablePlay() {
        binding.playButton.isEnabled = false

        if (scriptUri == null || videoUri == null) {
            return
        }

        var validatedScript: com.example.stimulationplayer.data.Script? = null
        try {
            val inputStream = contentResolver.openInputStream(scriptUri!!)
            validatedScript = inputStream?.use { ScriptValidator().validate(it) }
        } catch (e: ValidationException) {
            script = null
            timingModel = null
            Toast.makeText(this, "Script validation failed: ${e.message}", Toast.LENGTH_LONG).show()
            return
        } catch (e: Exception) {
            script = null
            timingModel = null
            Toast.makeText(this, "An error occurred while reading the script: ${e.message}", Toast.LENGTH_LONG).show()
            return
        }

        if (validatedScript != null) {
            script = validatedScript
            timingModel = TimingModel(validatedScript)
        } else {
            script = null
            timingModel = null
            Toast.makeText(this, "Failed to read script file.", Toast.LENGTH_LONG).show()
            return
        }

        binding.playButton.isEnabled = videoUri != null && script != null && areSoundsLoaded
    }

    private fun getFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    if (columnIndex >= 0) {
                        result = cursor.getString(columnIndex)
                    }
                }
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != null && cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result ?: "Unknown file"
    }

    private fun setupGestureDetector() {
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                toggleFullscreen()
                return true
            }
        })

        binding.playerView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }
    }

private fun toggleFullscreen() {
    isFullscreen = !isFullscreen

    // Get the main View of the Activity
    val decorView = window.decorView

    if (isFullscreen) {
        // Hide application controls
        binding.videoFileLabel.visibility = View.GONE
        binding.videoFileName.visibility = View.GONE
        binding.selectVideoButton.visibility = View.GONE
        binding.scriptFileLabel.visibility = View.GONE
        binding.scriptFileName.visibility = View.GONE
        binding.selectScriptButton.visibility = View.GONE
        binding.playButton.visibility = View.GONE
        binding.stopButton.visibility = View.GONE

        // !!! IMMERSIVE MODE CODE FOR ANDROID 9 (API 28) !!!
        // This combination of flags achieves hidden Status Bar and Navigation Bar
        decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY // Maintain immersive state
                or View.SYSTEM_UI_FLAG_FULLSCREEN           // Hide Status Bar
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION      // Hide Navigation Bar
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE        // Stabilize content size
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION // Extend content under Navigation Bar
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)     // Extend content under Status Bar

        // Hide the App Title Bar
        supportActionBar?.hide()

    } else {
        // Show application controls
        binding.videoFileLabel.visibility = View.VISIBLE
        binding.videoFileName.visibility = View.VISIBLE
        binding.selectVideoButton.visibility = View.VISIBLE
        binding.scriptFileLabel.visibility = View.VISIBLE
        binding.scriptFileName.visibility = View.VISIBLE
        binding.selectScriptButton.visibility = View.VISIBLE
        binding.playButton.visibility = View.VISIBLE
        binding.stopButton.visibility = View.VISIBLE

        // EXIT FULLSCREEN MODE
        // Reset flags to make system bars visible
        decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE

        // Show the App Title Bar
        supportActionBar?.show()
    }
}
}
