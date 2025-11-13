package com.example.stimulationplayer.data

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import java.io.InputStream

class ScriptValidator {

    private val gson = Gson()

    fun validate(inputStream: InputStream): Script {
        val script = try {
            val json = inputStream.bufferedReader().use { it.readText() }
            gson.fromJson(json, Script::class.java)
        } catch (e: JsonSyntaxException) {
            throw ValidationException("Invalid JSON format: ${e.message}")
        }

        // Rule: Check version
        if (script.version != "1.0") {
            throw ValidationException("Unsupported script version: ${script.version}")
        }

        // Rule: Check for empty sequences
        if (script.sequences.isEmpty()) {
            throw ValidationException("Script must contain at least one sequence.")
        }

        // Rule: Check sequence and step ordering and values
        var lastSeqId = 0
        script.sequences.forEach { seq ->
            if (seq.id <= lastSeqId) {
                throw ValidationException("Sequences must be ordered by strictly increasing ID. Found ID ${seq.id} after ${lastSeqId}.")
            }
            lastSeqId = seq.id

            if (seq.steps.isEmpty()) {
                throw ValidationException("Sequence ${seq.id} must contain at least one step.")
            }

            var lastStepId = 0
            seq.steps.forEach { step ->
                if (step.id <= lastStepId) {
                    throw ValidationException("Steps in sequence ${seq.id} must be ordered by strictly increasing ID. Found ID ${step.id} after ${lastStepId}.")
                }
                lastStepId = step.id

                // Rule: Check step duration
                if (step.duration_sec <= 0 || step.duration_sec < script.settings.min_step_duration_sec || step.duration_sec > script.settings.max_step_duration_sec) {
                    throw ValidationException("Step ${step.id} in sequence ${seq.id} has a duration of ${step.duration_sec}s, which is outside the allowed range of [${script.settings.min_step_duration_sec}, ${script.settings.max_step_duration_sec}].")
                }

                // Rule: Check metronome BPM
                if (step.metronome.enabled) {
                    if (step.metronome.bpm == null) {
                        throw ValidationException("Step ${step.id} in sequence ${seq.id} has metronome enabled but no BPM is provided.")
                    }
                    if (step.metronome.bpm < script.settings.min_bpm || step.metronome.bpm > script.settings.max_bpm) {
                        throw ValidationException("Step ${step.id} in sequence ${seq.id} has a BPM of ${step.metronome.bpm}, which is outside the allowed range of [${script.settings.min_bpm}, ${script.settings.max_bpm}].")
                    }
                }

                // Rule: Check touch mode
                val allowedTouchModes = setOf("none", "stroke", "tease", "squeeze", "tip_stroke", "tip_tease", "tip_squeeze", "edge")
                if (!allowedTouchModes.contains(step.touch.mode)) {
                    throw ValidationException("Step ${step.id} in sequence ${seq.id} has an invalid touch mode: '${step.touch.mode}'.")
                }
            }
        }

        return script
    }
}

class ValidationException(message: String) : Exception(message)
