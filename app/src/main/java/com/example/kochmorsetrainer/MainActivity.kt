package com.example.kochmorsetrainer

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var audioGenerator: MorseAudioGenerator
    private lateinit var kochManager: KochTrainerManager

    private var currentTargetString = ""
    private var isPlaying = false
    private var userAnswer = StringBuilder()
    private var charLength = 5

    private lateinit var choiceButtons: Array<Button>
    private lateinit var tvAnswer: TextView
    private lateinit var gridChars: LinearLayout  // 注意这里是 LinearLayout
    private lateinit var spinnerLevel: Spinner
    private lateinit var tvLengthValue: TextView
    private lateinit var tvCharSpeedValue: TextView
    private lateinit var tvEffSpeedValue: TextView
    private lateinit var tvFreqValue: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        try {
            audioGenerator = MorseAudioGenerator()
            kochManager = KochTrainerManager()

            initViews()
            setupLevelSpinner()
            setupSeekBars()
            setupButtons()
            updateCharsDisplay()
            updateChoiceButtons()
            generateCharGrid()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "初始化错误: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun initViews() {
        tvAnswer = findViewById(R.id.tvAnswer)
        gridChars = findViewById(R.id.gridChars)
        spinnerLevel = findViewById(R.id.spinnerLevel)
        tvLengthValue = findViewById(R.id.tvLengthValue)
        tvCharSpeedValue = findViewById(R.id.tvCharSpeedValue)
        tvEffSpeedValue = findViewById(R.id.tvEffSpeedValue)
        tvFreqValue = findViewById(R.id.tvFreqValue)

        choiceButtons = arrayOf(
            findViewById(R.id.btnChoice1),
            findViewById(R.id.btnChoice2),
            findViewById(R.id.btnChoice3),
            findViewById(R.id.btnChoice4)
        )
    }

    private fun setupLevelSpinner() {
        val levels = (2..kochManager.maxLevel).toList()
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, levels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerLevel.adapter = adapter
        spinnerLevel.setSelection(kochManager.currentLevel - 2)

        spinnerLevel.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                kochManager.setLevel(position + 2)
                updateCharsDisplay()
                updateChoiceButtons()
                generateCharGrid()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupSeekBars() {
        findViewById<SeekBar>(R.id.seekBarLength).apply {
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    charLength = progress + 3
                    tvLengthValue.text = charLength.toString()
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }

        findViewById<SeekBar>(R.id.seekBarCharSpeed).apply {
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val speed = progress + 5
                    tvCharSpeedValue.text = speed.toString()
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    updateAudioParams()
                }
            })
        }

        findViewById<SeekBar>(R.id.seekBarEffSpeed).apply {
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val speed = progress + 3
                    tvEffSpeedValue.text = speed.toString()
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    updateAudioParams()
                }
            })
        }

        findViewById<SeekBar>(R.id.seekBarFreq).apply {
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val freq = progress + 200
                    tvFreqValue.text = freq.toString()
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    updateAudioParams()
                }
            })
        }
    }

    private fun updateAudioParams() {
        val charSpeed = findViewById<SeekBar>(R.id.seekBarCharSpeed).progress + 5
        val effSpeed = findViewById<SeekBar>(R.id.seekBarEffSpeed).progress + 3
        val freq = findViewById<SeekBar>(R.id.seekBarFreq).progress + 200
        audioGenerator.setParams(charSpeed, effSpeed, freq)
    }

    private fun setupButtons() {
        val btnPlay = findViewById<Button>(R.id.btnPlay)
        val btnSubmit = findViewById<Button>(R.id.btnSubmit)
        val btnDelete = findViewById<Button>(R.id.btnDelete)
        val tvResult = findViewById<TextView>(R.id.tvResult)

        btnPlay.setOnClickListener {
            if (isPlaying) {
                audioGenerator.stop()
                isPlaying = false
                btnPlay.text = "播放摩尔斯码"
                btnSubmit.isEnabled = true
            } else {
                currentTargetString = kochManager.generateRandomString(charLength)
                userAnswer.clear()
                updateAnswerDisplay()

                isPlaying = true
                btnPlay.text = "停止"
                tvResult.text = ""
                btnSubmit.isEnabled = false

                audioGenerator.playText(currentTargetString) {
                    runOnUiThread {
                        isPlaying = false
                        btnPlay.text = "重新播放"
                        btnSubmit.isEnabled = true
                    }
                }
            }
        }

        for (button in choiceButtons) {
            button.setOnClickListener {
                if (userAnswer.length < charLength) {
                    userAnswer.append(button.text)
                    updateAnswerDisplay()
                    btnSubmit.isEnabled = userAnswer.isNotEmpty()
                }
            }
        }

        btnDelete.setOnClickListener {
            if (userAnswer.isNotEmpty()) {
                userAnswer.deleteCharAt(userAnswer.length - 1)
                updateAnswerDisplay()
                btnSubmit.isEnabled = userAnswer.isNotEmpty()
            }
        }

        btnSubmit.setOnClickListener {
            val userInput = userAnswer.toString()
            if (userInput.isEmpty()) return@setOnClickListener

            if (kochManager.checkAccuracy(userInput, currentTargetString)) {
                tvResult.text = "正确！"
                tvResult.setTextColor(getColor(android.R.color.holo_green_light))
            } else {
                tvResult.text = "错误，正确答案是: $currentTargetString"
                tvResult.setTextColor(getColor(android.R.color.holo_red_light))
            }

            btnSubmit.isEnabled = false
            btnPlay.text = "下一组"
            userAnswer.clear()
            updateAnswerDisplay()
        }
    }

    private fun generateCharGrid() {
        gridChars.removeAllViews()
        val chars = kochManager.currentChars
        val buttonsPerRow = 6

        var currentRow: LinearLayout? = null

        for ((index, char) in chars.withIndex()) {
            if (index % buttonsPerRow == 0) {
                currentRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    gravity = Gravity.CENTER
                }
                gridChars.addView(currentRow)
            }

            val btn = Button(this).apply {
                text = char.toString()
                textSize = 16f
                setTextColor(resources.getColor(android.R.color.white, theme))
                setBackgroundColor(resources.getColor(android.R.color.holo_blue_dark, theme))

                val params = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                ).apply {
                    setMargins(4, 4, 4, 4)
                }
                layoutParams = params

                setOnClickListener {
                    audioGenerator.playSingleChar(char)
                }
            }
            currentRow?.addView(btn)
        }
    }

    private fun updateCharsDisplay() {
        findViewById<TextView>(R.id.tvCurrentChars).text =
            " ${kochManager.currentChars.toCharArray().joinToString(" ")}"
    }

    private fun updateAnswerDisplay() {
        tvAnswer.text = userAnswer.toString()
    }

    private fun updateChoiceButtons() {
        val currentChars = kochManager.currentChars

        if (currentChars.length <= 4) {
            for (i in 0 until 4) {
                if (i < currentChars.length) {
                    choiceButtons[i].text = currentChars[i].toString()
                    choiceButtons[i].isEnabled = true
                } else {
                    choiceButtons[i].text = "-"
                    choiceButtons[i].isEnabled = false
                }
            }
        } else {
            val selected = currentChars.toList().shuffled().take(4)
            for (i in 0 until 4) {
                choiceButtons[i].text = selected[i].toString()
                choiceButtons[i].isEnabled = true
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioGenerator.release()
    }
}