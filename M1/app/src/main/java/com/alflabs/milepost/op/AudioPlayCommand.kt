package com.alflabs.milepost.op

/**
 * Command that indicates which audio file to play.
 *
 * The trigger id is only useful so that we can propagate it back to the UI later
 * and to identify whether the commands apply to the same trigger.
 * The persistableUri is the only information needed to actually play a file.
 * Neither the ids nor the uri are used by a Status command.
 */
data class AudioPlayCommand(
    val command: Command,
    val triggerId: Long,
    val persistableUri: String,
    val startIfPaused: Boolean = false,
) {
    enum class Command {
        QueueAdd,
        QueueRemove,
        Force,
        Pause,
        Unpause,
        Status,
    }
}
