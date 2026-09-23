package com.example.kochmorsetrainer

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import kotlin.math.PI
import kotlin.math.sin

/**
 * 摩尔斯音频引擎。
 *
 * 将整段文本一次性合成为单个 PCM 缓冲（含点划间的静音与每个音调的淡入淡出包络），
 * 再用一个 AudioTrack 播放。相比“每个音调新建一个 AudioTrack”的旧做法，这样：
 * 1. 时间间隔由采样点精确控制，杜绝因主线程 Handler 抖动导致的时长不稳；
 * 2. 每个音调首尾加入淡入淡出包络，避免波形突然起止产生的“咔哒”爆音。
 */
class MorseAudioGenerator {

    private val handler = Handler(Looper.getMainLooper())
    private var audioTrack: AudioTrack? = null

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
        val samples = buildSignal(text)
        if (samples.isEmpty()) {
            onFinished()
            return
        }
        playBuffer(samples, onFinished)
    }

    fun playSingleChar(char: Char) {
        stop()
        val samples = buildSignal(char.toString())
        if (samples.isEmpty()) return
        playBuffer(samples, null)
    }

    fun stop() {
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

    // ===== 合成 =====

    private fun msToSamples(ms: Double): Int = (ms * sampleRate / 1000.0).toInt()

    private fun totalDurationMs(text: String, charUnit: Double, effUnit: Double): Double {
        var total = 0.0
        for (char in text.uppercase()) {
            if (char == ' ') {
                total += effUnit * 4
                continue
            }
            val code = morseMap[char] ?: continue
            for (symbol in code) {
                total += if (symbol == '.') charUnit else charUnit * 3
                total += charUnit
            }
            total += charUnit * 2
        }
        return total
    }

    private fun buildSignal(text: String): ShortArray {
        val charUnit = 1200.0 / charWpm
        val effUnit = 1200.0 / effectiveWpm
        val totalSamples = msToSamples(totalDurationMs(text, charUnit, effUnit))
        if (totalSamples <= 0) return ShortArray(0)

        val samples = ShortArray(totalSamples)
        val cycleSamples = sampleRate / frequency
        var pos = 0

        for (char in text.uppercase()) {
            if (char == ' ') {
                pos += msToSamples(effUnit * 4)
                continue
            }
            val code = morseMap[char] ?: continue
            for (symbol in code) {
                val durationMs = if (symbol == '.') charUnit else charUnit * 3
                pos = addTone(samples, pos, msToSamples(durationMs), cycleSamples)
                pos += msToSamples(charUnit)
            }
            pos += msToSamples(charUnit * 2)
        }
        return samples
    }

    /** 在 samples 的 [start] 处写入一段带淡入淡出的正弦音，返回下一个写入位置 */
    private fun addTone(samples: ShortArray, start: Int, length: Int, cycleSamples: Double): Int {
        val fade = minOf(length / 3, msToSamples(FADE_MS))
        val end = start + length
        for (i in 0 until length) {
            var amplitude = 1.0
            if (fade > 0) {
                if (i < fade) {
                    amplitude = i.toDouble() / fade
                } else if (i >= length - fade) {
                    amplitude = (length - i).toDouble() / fade
                }
            }
            val value = sin(2.0 * PI * i / cycleSamples) * amplitude * GAIN
            samples[start + i] = (value * Short.MAX_VALUE).toInt().toShort()
        }
        return end
    }

    // ===== 播放 =====

    private fun playBuffer(samples: ShortArray, onFinished: (() -> Unit)?) {
        try {
            audioTrack?.release()
            audioTrack = AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                samples.size * 2,
                AudioTrack.MODE_STATIC
            )
            audioTrack?.write(samples, 0, samples.size)
            audioTrack?.play()
        } catch (e: Exception) {
            e.printStackTrace()
            onFinished?.invoke()
            return
        }

        val durationMs = samples.size.toLong() * 1000 / sampleRate
        handler.postDelayed({
            onFinished?.invoke()
        }, durationMs)
    }

    private companion object {
        const val FADE_MS = 4.0
        const val GAIN = 0.95
    }
}
