package com.example.kochmorsetrainer

import android.media.AudioManager
import android.media.AudioTrack
import android.media.AudioFormat
import android.os.Handler
import android.os.Looper
import kotlin.math.sin

class MorseAudioGenerator {

    private val handler = Handler(Looper.getMainLooper())
    private var audioTrack: AudioTrack? = null
    private var isPlaying = false

    private val morseMap = mapOf(
        'A' to ".-",    'B' to "-...",  'C' to "-.-.",  'D' to "-..",
        'E' to ".",     'F' to "..-.",  'G' to "--.",   'H' to "....",
        'I' to "..",    'J' to ".---",  'K' to "-.-",   'L' to ".-..",
        'M' to "--",    'N' to "-.",    'O' to "---",   'P' to ".--.",
        'Q' to "--.-",  'R' to ".-.",   'S' to "...",   'T' to "-",
        'U' to "..-",   'V' to "...-",  'W' to ".--",   'X' to "-..-",
        'Y' to "-.--",  'Z' to "--..",
        '0' to "-----", '1' to ".----", '2' to "..---", '3' to "...--",
        '4' to "....-", '5' to ".....", '6' to "-....", '7' to "--...",
        '8' to "---..", '9' to "----.",
        '.' to ".-.-.-", ',' to "--..--", '?' to "..--..", '/' to "-..-."
    )

    private var charWpm = 20
    private var effectiveWpm = 10
    private var frequency = 600.0
    private val sampleRate = 44100

    fun setParams(charSpeed: Int, effSpeed: Int, freq: Int) {
        this.charWpm = charSpeed.coerceIn(5, 50)
        this.effectiveWpm = effSpeed.coerceIn(3, charWpm)
        this.frequency = freq.toDouble().coerceIn(200.0, 1600.0)
    }

    fun playText(text: String, onFinished: () -> Unit) {
        stop()
        isPlaying = true

        val charUnit = 1200L / charWpm
        val effUnit = 1200L / effectiveWpm
        var currentTime = 0L

        for (char in text.uppercase()) {
            if (char == ' ') {
                currentTime += effUnit * 4
                continue
            }

            val code = morseMap[char] ?: continue

            for (symbol in code) {
                val duration = if (symbol == '.') charUnit else charUnit * 3
                scheduleTone(duration, currentTime)
                currentTime += duration + charUnit
            }
            currentTime += charUnit * 2
        }

        handler.postDelayed({
            isPlaying = false
            onFinished()
        }, currentTime)
    }

    fun playSingleChar(char: Char) {
        stop()
        isPlaying = true

        val code = morseMap[char.uppercaseChar()] ?: return
        val charUnit = 1200L / charWpm
        var currentTime = 0L

        for (symbol in code) {
            val duration = if (symbol == '.') charUnit else charUnit * 3
            scheduleTone(duration, currentTime)
            currentTime += duration + charUnit
        }

        handler.postDelayed({
            isPlaying = false
        }, currentTime)
    }

    private fun scheduleTone(durationMs: Long, delayMs: Long) {
        val numSamples = (durationMs * sampleRate / 1000).toInt()
        if (numSamples <= 0) return

        val samples = DoubleArray(numSamples)
        val generated = ByteArray(2 * numSamples)

        for (i in 0 until numSamples) {
            samples[i] = sin(2.0 * Math.PI * i / (sampleRate / frequency))
            val val16 = (samples[i] * 32767).toInt().toShort()
            generated[2 * i] = (val16.toInt() and 0x00FF).toByte()
            generated[2 * i + 1] = (val16.toInt() shr 8).toByte()
        }

        handler.postDelayed({
            if (!isPlaying) return@postDelayed

            try {
                // 先释放旧的
                audioTrack?.stop()
                audioTrack?.release()

                audioTrack = AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    generated.size,
                    AudioTrack.MODE_STATIC
                )
                audioTrack?.write(generated, 0, generated.size)
                audioTrack?.play()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, delayMs)
    }

    fun stop() {
        isPlaying = false
        handler.removeCallbacksAndMessages(null)
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        audioTrack = null
    }

    fun release() {
        stop()
    }
}