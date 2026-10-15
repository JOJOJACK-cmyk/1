package com.jojojack.mathalarm.ring

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import com.jojojack.mathalarm.alarm.AlarmService
import com.jojojack.mathalarm.ui.MathAlarmTheme

/** 잠금화면 위에 표시되는 알람 화면. 문제를 모두 풀어야 알람이 꺼진다. */
class AlarmRingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOverLockScreen()

        val problemCount = AlarmService.ringingProblemCount
        if (problemCount == null) {
            finish()
            return
        }

        // 뒤로 가기로는 알람을 끌 수 없다.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = Unit
        })

        setContent {
            MathAlarmTheme {
                MathChallengeScreen(
                    problemCount = problemCount,
                    onSolved = {
                        stopService(Intent(this, AlarmService::class.java))
                        finish()
                    },
                )
            }
        }
    }

    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}
