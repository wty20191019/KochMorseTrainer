package com.example.kochmorsetrainer

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView

/**
 * 单字·听音选字页：自动播放一个随机字符，用户从 4 个候选按钮中选出，
 * 显示对错后自动进入下一题。
 */
class SingleListenFragment : BaseTrainerFragment() {

    override val state get() = vm.listen

    private lateinit var tvAnswer: TextView
    private lateinit var tvResult: TextView
    private lateinit var choiceButtons: Array<Button>
    private var target = ""
    private var answering = false
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_single_listen, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initAudio()
        bindSettings(view)

        tvAnswer = view.findViewById(R.id.tvAnswer)
        tvResult = view.findViewById(R.id.tvResult)
        choiceButtons = arrayOf(
            view.findViewById(R.id.btnChoice1),
            view.findViewById(R.id.btnChoice2),
            view.findViewById(R.id.btnChoice3),
            view.findViewById(R.id.btnChoice4)
        )
        for (button in choiceButtons) {
            button.setOnClickListener { onAnswer(button.text.firstOrNull()) }
        }
        view.findViewById<Button>(R.id.btnReplay).setOnClickListener { replay() }

        startRound()
    }

    override fun onLevelChanged() {
        startRound()
    }

    private fun startRound() {
        if (!isAdded) return
        handler.removeCallbacksAndMessages(null)
        answering = false
        target = koch.generateRandomString(1)
        tvResult.text = ""
        tvAnswer.text = ""
        updateChoiceButtons()
        playTarget()
    }

    private fun playTarget() {
        if (target.isEmpty()) return
        audio?.stop()
        audio?.playText(target) {}
    }

    private fun replay() {
        if (target.isEmpty() || answering) return
        playTarget()
    }

    private fun updateChoiceButtons() {
        val chars = koch.currentChars.toList()
        if (chars.size <= 4) {
            for (i in 0 until 4) {
                if (i < chars.size) {
                    choiceButtons[i].text = chars[i].toString()
                    choiceButtons[i].isEnabled = true
                } else {
                    choiceButtons[i].text = "-"
                    choiceButtons[i].isEnabled = false
                }
            }
        } else {
            val others = chars.filter { it != target[0] }.shuffled().take(3)
            val options = (others + target[0]).shuffled()
            choiceButtons.forEachIndexed { i, button ->
                button.text = options[i].toString()
                button.isEnabled = true
            }
        }
    }

    private fun onAnswer(ch: Char?) {
        if (ch == null || answering || target.isEmpty()) return
        answering = true
        tvAnswer.text = ch.toString()

        if (ch.equals(target[0], ignoreCase = true)) {
            tvResult.text = "正确！"
            tvResult.setTextColor(requireContext().getColor(android.R.color.holo_green_light))
        } else {
            tvResult.text = "错误，正确答案是: $target"
            tvResult.setTextColor(requireContext().getColor(android.R.color.holo_red_light))
        }

        handler.postDelayed({ if (isAdded) startRound() }, NEXT_DELAY_MS)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacksAndMessages(null)
    }

    override fun onDestroyView() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroyView()
    }

    private companion object {
        const val NEXT_DELAY_MS = 1200L
    }
}
