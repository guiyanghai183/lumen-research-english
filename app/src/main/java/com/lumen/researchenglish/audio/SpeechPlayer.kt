package com.lumen.researchenglish.audio

import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import java.io.File

class SpeechPlayer(private val context: Context) {
    private var player: MediaPlayer? = null
    private val files = mutableListOf<File>()
    private var completion: (() -> Unit)? = null
    private var progress: ((Float) -> Unit)? = null
    private val progressHandler = Handler(Looper.getMainLooper())
    private var activePartIndex = 0
    private var nextPartIndex = 0
    private var partWeights = listOf(1f)
    private var partStartWeights = listOf(0f)
    private var totalWeight = 1f
    private var queueFinished = false

    private val progressTick = object : Runnable {
        override fun run() {
            val currentPlayer = player ?: return
            val duration = currentPlayer.duration.coerceAtLeast(1)
            val partProgress = currentPlayer.currentPosition.toFloat() / duration.toFloat()
            val completedWeight = partStartWeights.getOrElse(activePartIndex) { 0f }
            val activeWeight = partWeights.getOrElse(activePartIndex) { 1f }
            progress?.invoke(
                ((completedWeight + activeWeight * partProgress) / totalWeight)
                    .coerceIn(0f, 1f),
            )
            if (currentPlayer.isPlaying) progressHandler.postDelayed(this, 60L)
        }
    }

    fun startStream(
        weights: List<Float>,
        onProgress: (Float) -> Unit,
        onComplete: () -> Unit,
    ) {
        stop()
        partWeights = weights.ifEmpty { listOf(1f) }.map { it.coerceAtLeast(1f) }
        var accumulatedWeight = 0f
        partStartWeights = partWeights.map { weight ->
            val startWeight = accumulatedWeight
            accumulatedWeight += weight
            startWeight
        }
        totalWeight = accumulatedWeight.coerceAtLeast(1f)
        progress = onProgress
        completion = onComplete
        queueFinished = false
    }

    /** Adds audio as soon as each TTS request completes and starts the first part immediately. */
    fun enqueue(audio: ByteArray) {
        if (completion == null) return
        val file = File(context.cacheDir, "lumen-speech-${System.nanoTime()}-${files.size}.mp3")
        file.writeBytes(audio)
        files += file
        if (player == null && nextPartIndex < files.size) playPart(nextPartIndex)
    }

    fun finishStream() {
        queueFinished = true
        if (player == null && nextPartIndex >= files.size) completePlayback()
    }

    fun play(
        audioParts: List<ByteArray>,
        onProgress: (Float) -> Unit,
        onComplete: () -> Unit,
    ) {
        require(audioParts.isNotEmpty()) { "No speech audio was generated." }
        startStream(List(audioParts.size) { 1f }, onProgress, onComplete)
        audioParts.forEach(::enqueue)
        finishStream()
    }

    fun stop() {
        progressHandler.removeCallbacks(progressTick)
        player?.setOnCompletionListener(null)
        player?.setOnErrorListener(null)
        player?.release()
        player = null
        files.forEach { runCatching { it.delete() } }
        files.clear()
        completion = null
        progress = null
        activePartIndex = 0
        nextPartIndex = 0
        partWeights = listOf(1f)
        partStartWeights = listOf(0f)
        totalWeight = 1f
        queueFinished = false
    }

    private fun playPart(index: Int) {
        player?.release()
        player = null
        if (index >= files.size) {
            if (queueFinished) completePlayback()
            return
        }
        activePartIndex = index
        nextPartIndex = index + 1
        player = MediaPlayer().apply {
            setDataSource(files[index].absolutePath)
            setOnCompletionListener { playPart(nextPartIndex) }
            setOnErrorListener { _, _, _ ->
                val done = completion
                stop()
                done?.invoke()
                true
            }
            prepare()
            start()
            progressHandler.removeCallbacks(progressTick)
            progressHandler.post(progressTick)
        }
    }

    private fun completePlayback() {
        val done = completion
        progress?.invoke(1f)
        progressHandler.removeCallbacks(progressTick)
        player?.release()
        player = null
        files.forEach { runCatching { it.delete() } }
        files.clear()
        completion = null
        progress = null
        nextPartIndex = 0
        partWeights = listOf(1f)
        partStartWeights = listOf(0f)
        totalWeight = 1f
        queueFinished = false
        done?.invoke()
    }
}
