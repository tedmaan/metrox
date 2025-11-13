package com.example.stimulationplayer

import android.os.Handler
import android.os.Looper
import androidx.media3.common.Player
import com.example.stimulationplayer.data.TimedStep
import com.example.stimulationplayer.data.TimingModel

class SyncEngine(
    private val player: Player,
    private val timingModel: TimingModel,
    private val audioEngine: AudioEngine,
    private val onOverlayUpdate: (String) -> Unit
) {
    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false
    private var lastStepId: Int? = null
    private var nextMetronomeClickTimeMs: Long = -1

    private val syncRunnable = object : Runnable {
        override fun run() {
            if (!isRunning) return

            val currentPosition = player.currentPosition
            val currentStep = timingModel.timedSteps.find {
                currentPosition >= it.startTimeMs && currentPosition < it.endTimeMs
            }

            if (currentStep != null) {
                // Check for step change
                if (currentStep.originalStep.id != lastStepId) {
                    onStepChanged(currentStep)
                }
                // Handle metronome for the current step
                handleMetronome(currentStep, currentPosition)
            } else {
                // No active step, reset state
                if (lastStepId != null) {
                    onOverlayUpdate("")
                    lastStepId = null
                }
            }


            if (isRunning) {
                handler.postDelayed(this, SYNC_INTERVAL_MS)
            }
        }
    }

    private fun onStepChanged(newStep: TimedStep) {
        lastStepId = newStep.originalStep.id
        onOverlayUpdate(newStep.originalStep.touch.notes)

        if (newStep.isFirstInSequence) {
            audioEngine.playSequenceBeep()
        }
        audioEngine.playStepBeep()

        // Reset metronome for the new step
        if (newStep.originalStep.metronome.enabled && newStep.originalStep.metronome.bpm != null) {
            val bpm = newStep.originalStep.metronome.bpm
            val interval = 60000.0 / bpm
            // First click is at the start of the step
            nextMetronomeClickTimeMs = newStep.startTimeMs
        } else {
            nextMetronomeClickTimeMs = -1
        }
    }

    private fun handleMetronome(currentStep: TimedStep, currentPosition: Long) {
        if (!currentStep.originalStep.metronome.enabled || currentStep.originalStep.metronome.bpm == null) {
            return
        }

        if (nextMetronomeClickTimeMs != -1L && currentPosition >= nextMetronomeClickTimeMs) {
            audioEngine.playMetronomeClick()
            val bpm = currentStep.originalStep.metronome.bpm
            val interval = 60000.0 / bpm
            // Schedule the next click
            while(nextMetronomeClickTimeMs <= currentPosition){
                nextMetronomeClickTimeMs += interval.toLong()
            }
        }
    }

    fun start() {
        if (isRunning) return
        isRunning = true
        lastStepId = null
        nextMetronomeClickTimeMs = -1
        handler.post(syncRunnable)
    }

    fun stop() {
        isRunning = false
        handler.removeCallbacks(syncRunnable)
        onOverlayUpdate("")
    }

    companion object {
        private const val SYNC_INTERVAL_MS = 20L // 50Hz
    }
}
