package com.jojojack.mathalarm

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.jojojack.mathalarm.alarm.AlarmService
import com.jojojack.mathalarm.ring.AlarmRingActivity
import com.jojojack.mathalarm.ui.AlarmListScreen
import com.jojojack.mathalarm.ui.MathAlarmTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MathAlarmTheme {
                AlarmListScreen()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 알람 알림을 지웠더라도 앱을 열면 문제 풀이 화면으로 보낸다.
        if (AlarmService.ringingProblemCount != null) {
            startActivity(Intent(this, AlarmRingActivity::class.java))
        }
    }
}
