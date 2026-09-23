package com.example.kochmorsetrainer

import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels

/**
 * 三个训练页的公共基类：统一持有音频引擎、Koch 管理器，并绑定“等级 + 音频参数”设置区。
 * 每个 Fragment 实例各自独立，设置值来自 Activity 级 ViewModel 中对应的 PageState。
 */
abstract class BaseTrainerFragment : Fragment() {

    protected val vm: TrainerViewModel by activityViewModels()
    protected val koch = KochTrainerManager()
    protected var audio: MorseAudioGenerator? = null

    /** 当前页对应的独立状态 */
    protected abstract val state: PageState

    /** 子类在 onViewCreated 开头调用：创建音频引擎并应用当前页参数 */
    protected fun initAudio() {
        audio = MorseAudioGenerator().also {
            it.setParams(state.charWpm, state.effWpm, state.freq)
        }
    }

    /** 绑定公共设置区的等级与三个音频滑条 */
    protected fun bindSettings(root: View) {
        koch.setLevel(state.level)

        val spinner = root.findViewById<Spinner>(R.id.spinnerLevel)
        val levels = (2..koch.maxLevel).toList()
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, levels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
        spinner.setSelection(state.level - 2)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                state.level = position + 2
                koch.setLevel(state.level)
                updateCharsDisplay(root)
                onLevelChanged()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val tvChar = root.findViewById<TextView>(R.id.tvCharSpeedValue)
        val tvEff = root.findViewById<TextView>(R.id.tvEffSpeedValue)
        val tvFreq = root.findViewById<TextView>(R.id.tvFreqValue)
        tvChar.text = state.charWpm.toString()
        tvEff.text = state.effWpm.toString()
        tvFreq.text = state.freq.toString()

        root.findViewById<SeekBar>(R.id.seekBarCharSpeed).apply {
            max = 45
            progress = state.charWpm - 5
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    state.charWpm = progress + 5
                    tvChar.text = state.charWpm.toString()
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) = applyAudioParams()
            })
        }

        root.findViewById<SeekBar>(R.id.seekBarEffSpeed).apply {
            max = 35
            progress = state.effWpm - 3
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    state.effWpm = progress + 3
                    tvEff.text = state.effWpm.toString()
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) = applyAudioParams()
            })
        }

        root.findViewById<SeekBar>(R.id.seekBarFreq).apply {
            max = 1400
            progress = state.freq - 200
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    state.freq = progress + 200
                    tvFreq.text = state.freq.toString()
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) = applyAudioParams()
            })
        }

        updateCharsDisplay(root)
    }

    protected fun applyAudioParams() {
        audio?.setParams(state.charWpm, state.effWpm, state.freq)
    }

    protected fun updateCharsDisplay(root: View) {
        root.findViewById<TextView>(R.id.tvCurrentChars).text =
            " " + koch.currentChars.toCharArray().joinToString(" ")
    }

    /** 等级变化后子类刷新自己的内容（候选按钮 / 字符网格等） */
    protected abstract fun onLevelChanged()

    override fun onPause() {
        super.onPause()
        audio?.stop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        audio?.release()
        audio = null
    }
}
