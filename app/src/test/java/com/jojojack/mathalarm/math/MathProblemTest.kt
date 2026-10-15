package com.jojojack.mathalarm.math

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class MathProblemTest {
    @Test
    fun answersMatchQuestions() {
        val random = Random(42)
        repeat(1_000) {
            val problem = MathProblem.random(random)
            val (a, op, b) = problem.question.split(" ")
            val expected = when (op) {
                "+" -> a.toInt() + b.toInt()
                "−" -> a.toInt() - b.toInt()
                "×" -> a.toInt() * b.toInt()
                else -> error("unknown operator $op")
            }
            assertEquals(expected, problem.answer)
            assertTrue("답은 0보다 커야 함: ${problem.question}", problem.answer > 0)
            assertTrue("답은 4자리 이하: ${problem.question}", problem.answer < 10_000)
        }
    }
}
