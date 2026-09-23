package com.example.kochmorsetrainer

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout

/** 单字·选字听音页：列出当前等级的字符，点击任意字符即播放其摩尔斯音 */
class SingleTapFragment : BaseTrainerFragment() {

    override val state get() = vm.tap

    private lateinit var gridChars: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_single_tap, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initAudio()
        bindSettings(view)
        gridChars = view.findViewById(R.id.gridChars)
        generateGrid()
    }

    override fun onLevelChanged() {
        generateGrid()
    }

    private fun generateGrid() {
        gridChars.removeAllViews()
        val chars = koch.currentChars
        val buttonsPerRow = 6

        var currentRow: LinearLayout? = null

        for ((index, char) in chars.withIndex()) {
            if (index % buttonsPerRow == 0) {
                currentRow = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    gravity = Gravity.CENTER
                }
                gridChars.addView(currentRow)
            }

            val btn = Button(requireContext()).apply {
                text = char.toString()
                textSize = 16f
                setTextColor(resources.getColor(android.R.color.white, requireContext().theme))
                setBackgroundColor(resources.getColor(android.R.color.holo_blue_dark, requireContext().theme))

                val params = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                ).apply {
                    setMargins(4, 4, 4, 4)
                }
                layoutParams = params

                setOnClickListener {
                    audio?.playSingleChar(char)
                }
            }
            currentRow?.addView(btn)
        }
    }
}
