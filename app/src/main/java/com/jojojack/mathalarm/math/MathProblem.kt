package com.jojojack.mathalarm.math

import kotlin.random.Random

/** 알람을 끄기 위해 풀어야 하는 간단한 산수 문제. */
data class MathProblem(val question: String, val answer: Int) {
    companion object {
        fun random(random: Random = Random.Default): MathProblem = when (random.nextInt(3)) {
            0 -> {
                val a = random.nextInt(10, 100)
                val b = random.nextInt(10, 100)
                MathProblem("$a + $b", a + b)
            }
            1 -> {
                val a = random.nextInt(20, 100)
                val b = random.nextInt(10, a)
                MathProblem("$a − $b", a - b)
            }
            else -> {
                val a = random.nextInt(2, 10)
                val b = random.nextInt(11, 20)
                MathProblem("$a × $b", a * b)
            }
        }
    }
}
