package com.example.stimulationplayer.data

data class TimedStep(
    val originalStep: Step,
    val sequenceId: Int,
    val isFirstInSequence: Boolean,
    val startTimeMs: Long,
    val endTimeMs: Long
)

class TimingModel(private val script: Script) {

    val timedSteps: List<TimedStep>

    init {
        timedSteps = computeTiming()
    }

    private fun computeTiming(): List<TimedStep> {
        val flattenedSteps = mutableListOf<TimedStep>()
        var cumulativeTimeMs = 0L

        script.sequences.forEach { sequence ->
            sequence.steps.forEachIndexed { index, step ->
                val durationMs = (step.duration_sec * 1000).toLong()
                val isFirst = index == 0

                flattenedSteps.add(
                    TimedStep(
                        originalStep = step,
                        sequenceId = sequence.id,
                        isFirstInSequence = isFirst,
                        startTimeMs = cumulativeTimeMs,
                        endTimeMs = cumulativeTimeMs + durationMs
                    )
                )

                cumulativeTimeMs += durationMs
            }
        }
        return flattenedSteps
    }
}
