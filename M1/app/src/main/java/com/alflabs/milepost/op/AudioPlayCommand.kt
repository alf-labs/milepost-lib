package com.alflabs.milepost.op

/**
 * Command that indicates which audio file to play.
 *
 * The ids only used as unique identifiers to differentiate files.
 * The persistableUri is the only information needed to actually play a file.
 * Neither the ids nor the uri are used by a Status command.
 */
data class AudioPlayCommand(
    val command: Command,
    val triggerId: Long,
    val audioFileId: Long,
    val persistableUri: String,
) {
    enum class Command {
        QueueAdd,
        QueueRemove,
        Force,
        Status,
    }
}
