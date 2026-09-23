package com.example.kochmorsetrainer

import kotlin.random.Random

class KochTrainerManager {

    private val kochSequence = "KMURESAWPTLNOIYGJQCXZHBFV0123456789.,?"

    var currentLevel = 2
        private set

    val currentChars: String
        get() = kochSequence.substring(0, currentLevel.coerceIn(2, kochSequence.length))

    val maxLevel: Int
        get() = kochSequence.length

    fun setLevel(level: Int) {
        currentLevel = level.coerceIn(2, kochSequence.length)
    }

    fun generateRandomString(length: Int): String {
        return (1..length)
            .map { currentChars[Random.nextInt(0, currentChars.length)] }
            .joinToString("")
    }

    fun checkAccuracy(input: String, target: String): Boolean {
        return input.trim().equals(target, ignoreCase = true)
    }

    fun levelUp() {
        if (currentLevel < kochSequence.length) {
            currentLevel++
        }
    }

    fun reset() {
        currentLevel = 2
    }
}