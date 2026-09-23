package com.example.kochmorsetrainer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView

/** 整串听认页：随机出一串字符，播放摩尔斯码后由用户逐个选字作答 */
class SequenceFragment : BaseTrainerFragment() {

    override val state get() = vm.sequence

    private lateinit var tvAnswer: TextView
    private lateinit var tvResult: TextView
    private lateinit var btnPlay: Button
    private lateinit var btnSubmit: Button
    private lateinit var choiceContainer: LinearLayout
    private var isPlaying = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_sequence, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initAudio()
        bindSettings(view)

        tvAnswer = view.findViewById(R.id.tvAnswer)
        tvResult = view.findViewById(R.id.tvResult)
        btnPlay = view.findViewById(R.id.btnPlay)
        btnSubmit = view.findViewById(R.id.btnSubmit)
        choiceContainer = view.findViewById(R.id.choiceContainer)

        setupCharLength(view)
        setupButtons(view)
        updateChoiceButtons()
        restore()
    }

    override fun onLevelChanged() {
        updateChoiceButtons()
    }

    private fun setupCharLength(root: View) {
        val tvLength = root.findViewById<TextView>(R.id.tvLengthValue)
        tvLength.text = state.charLength.toString()
        root.findViewById<SeekBar>(R.id.seekBarLength).apply {
            max = 9
            progress = state.charLength - 1
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    state.charLength = progress + 1
                    tvLength.text = state.charLength.toString()
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
    }

    private fun setupButtons(root: View) {
        btnPlay.setOnClickListener {
            if (isPlaying) {
                audio?.stop()
                isPlaying = false
                btnPlay.text = "播放摩尔斯码"
                btnSubmit.isEnabled = true
            } else {
                state.target = koch.generateRandomString(state.charLength)
                state.answer.clear()
                updateAnswerDisplay()

                isPlaying = true
                btnPlay.text = "停止"
                tvResult.text = ""
                btnSubmit.isEnabled = false

                audio?.playText(state.target) {
                    activity?.runOnUiThread {
                        isPlaying = false
                        btnPlay.text = "重新播放"
                        btnSubmit.isEnabled = true
                    }
                }
            }
        }

        root.findViewById<Button>(R.id.btnDelete).setOnClickListener {
            if (state.answer.isNotEmpty()) {
                state.answer.deleteCharAt(state.answer.length - 1)
                updateAnswerDisplay()
                btnSubmit.isEnabled = state.answer.isNotEmpty()
            }
        }

        btnSubmit.setOnClickListener {
            val userInput = state.answer.toString()
            if (userInput.isEmpty()) return@setOnClickListener

            if (koch.checkAccuracy(userInput, state.target)) {
                tvResult.text = "正确！"
                tvResult.setTextColor(requireContext().getColor(android.R.color.holo_green_light))
            } else {
                tvResult.text = "错误，正确答案是: ${state.target}"
                tvResult.setTextColor(requireContext().getColor(android.R.color.holo_red_light))
            }

            btnSubmit.isEnabled = false
            btnPlay.text = "下一组"
            state.answer.clear()
            updateAnswerDisplay()
        }
    }

    private fun updateAnswerDisplay() {
        tvAnswer.text = state.answer.toString()
    }

    private fun updateChoiceButtons() {
        // 候选固定为当前等级的全部字符
        val labels = koch.currentChars.map { it.toString() }
        renderChoiceButtons(choiceContainer, labels) { label ->
            if (state.answer.length < state.charLength) {
                state.answer.append(label)
                updateAnswerDisplay()
                btnSubmit.isEnabled = state.answer.isNotEmpty()
            }
        }
    }

    /** 旋转 / 切页回来后恢复上一次的作答显示 */
    private fun restore() {
        updateAnswerDisplay()
        if (state.target.isNotEmpty()) {
            btnPlay.text = "重新播放"
            btnSubmit.isEnabled = state.answer.isNotEmpty()
        }
    }
}
