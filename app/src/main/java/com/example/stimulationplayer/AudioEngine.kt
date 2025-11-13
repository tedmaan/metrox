package com.example.stimulationplayer

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
class AudioEngine(context: Context, private val onReady: () -> Unit) {

    private val soundPool: SoundPool
    private var stepBeepId: Int = 0
    private var sequenceBeepId: Int = 0
    private var metronomeClickId: Int = 0
    private var soundsLoaded = 0

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(3)
            .setAudioAttributes(audioAttributes)
            .build()

        soundPool.setOnLoadCompleteListener { _, _, status ->
            if (status == 0) {
                soundsLoaded++
                if (soundsLoaded == 3) {
                    onReady()
                }
            }
        }

        stepBeepId = soundPool.load(context, R.raw.step_beep, 1)
        sequenceBeepId = soundPool.load(context, R.raw.sequence_beep, 1)
        metronomeClickId = soundPool.load(context, R.raw.metronome_click, 1)
    }

    fun playStepBeep() {
        soundPool.play(stepBeepId, 1f, 1f, 1, 0, 1f)
    }

    fun playSequenceBeep() {
        soundPool.play(sequenceBeepId, 1f, 1f, 1, 0, 1f)
    }

    fun playMetronomeClick() {
        soundPool.play(metronomeClickId, 1f, 1f, 1, 0, 1f)
    }

    fun release() {
        soundPool.release()
    }
}
