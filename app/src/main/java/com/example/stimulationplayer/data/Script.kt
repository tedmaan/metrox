package com.example.stimulationplayer.data

data class Script(
    val version: String,
    val trial_id: String,
    val description: String,
    val settings: Settings,
    val sequences: List<Sequence>
)

data class Settings(
    val min_bpm: Int,
    val max_bpm: Int,
    val min_step_duration_sec: Int,
    val max_step_duration_sec: Int
)

data class Sequence(
    val id: Int,
    val label: String,
    val purpose: String,
    val steps: List<Step>
)

data class Step(
    val id: Int,
    val duration_sec: Double,
    val metronome: Metronome,
    val touch: Touch
)

data class Metronome(
    val enabled: Boolean,
    val bpm: Int?
)

data class Touch(
    val mode: String,
    val notes: String
)
