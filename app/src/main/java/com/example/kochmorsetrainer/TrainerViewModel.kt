package com.example.kochmorsetrainer

import androidx.lifecycle.ViewModel

/** 单个页面独立持有的设置状态 */
open class PageState {
    var level = 2
    var charWpm = 20
    var effWpm = 10
    var freq = 600
}

/** 整串听认页额外持有的出题状态 */
class SequenceState : PageState() {
    var charLength = 5
    var target = ""
    val answer = StringBuilder()
}

/**
 * Activity 级 ViewModel：让三个页面各自的设置与出题状态在切页、旋转屏幕后依然保留。
 * 每页状态完全独立，互不影响。
 */
class TrainerViewModel : ViewModel() {
    val sequence = SequenceState()
    val listen = PageState()
    val tap = PageState()
}
