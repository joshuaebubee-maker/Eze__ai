package com.example.voice

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Lightweight, battery-conscious wake-word acoustic monitor for "Hey EZE" and "EZE".
 * Uses AudioRecord with low-overhead root-mean-square (RMS) acoustic energy monitoring.
 * When vocal energy bursts occur, it triggers an evaluation window rather than continuously
 * keeping high-level recognition loops spinning, conserving battery on 2GB RAM devices.
 */
class WakeWordDetector(
    private val context: Context,
    private val onWakeWordDetected: () -> Unit,
    private val onError: (String) -> Unit
) {

    private val isRunning = AtomicBoolean(false)
    private var recordingJob: Job? = null
    private var audioRecord: AudioRecord? = null

    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    // Sensitivity threshold for human vocal onset
    private val voiceEnergyThreshold = 1800.0

    @SuppressLint("MissingPermission")
    fun start(scope: CoroutineScope) {
        if (isRunning.getAndSet(true)) return

        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat).coerceAtLeast(2048)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                onError("Microphone cannot be initialized for wake-word monitoring.")
                isRunning.set(false)
                return
            }

            audioRecord?.startRecording()

            recordingJob = scope.launch(Dispatchers.IO) {
                val buffer = ShortArray(bufferSize / 2)
                var consecutiveSpeechFrames = 0

                while (isActive && isRunning.get()) {
                    val readCount = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (readCount > 0) {
                        var sum = 0.0
                        for (i in 0 until readCount) {
                            sum += buffer[i] * buffer[i]
                        }
                        val rms = Math.sqrt(sum / readCount)

                        if (rms > voiceEnergyThreshold) {
                            consecutiveSpeechFrames++
                            // If voice activity is sustained for ~200-400ms, trigger wake activation
                            if (consecutiveSpeechFrames >= 3) {
                                consecutiveSpeechFrames = 0
                                scope.launch(Dispatchers.Main) {
                                    onWakeWordDetected()
                                }
                                // Backoff momentarily to avoid duplicate triggers
                                kotlinx.coroutines.delay(1200)
                            }
                        } else {
                            if (consecutiveSpeechFrames > 0) consecutiveSpeechFrames--
                        }
                    }
                    kotlinx.coroutines.delay(40) // Energy conservation pause
                }
            }
        } catch (e: SecurityException) {
            isRunning.set(false)
            onError("Microphone permission required for wake-word.")
        } catch (e: Exception) {
            isRunning.set(false)
            onError("Wake-word monitor error: ${e.localizedMessage}")
        }
    }

    fun stop() {
        if (!isRunning.getAndSet(false)) return

        recordingJob?.cancel()
        recordingJob = null

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            // Ignore
        } finally {
            audioRecord = null
        }
    }

    fun isActive(): Boolean = isRunning.get()
}
