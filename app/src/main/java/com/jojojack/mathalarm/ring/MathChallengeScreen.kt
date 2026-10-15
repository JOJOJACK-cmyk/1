package com.jojojack.mathalarm.ring

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jojojack.mathalarm.data.AlarmFormat
import com.jojojack.mathalarm.math.MathProblem
import kotlinx.coroutines.delay
import java.util.Calendar

private const val MAX_INPUT_LENGTH = 4

@Composable
fun MathChallengeScreen(problemCount: Int, onSolved: () -> Unit) {
    var solved by rememberSaveable { mutableIntStateOf(0) }
    var problem by remember { mutableStateOf(MathProblem.random()) }
    var input by rememberSaveable { mutableStateOf("") }
    var feedback by remember { mutableStateOf<String?>(null) }
    var now by remember { mutableStateOf(Calendar.getInstance()) }

    LaunchedEffect(Unit) {
        while (true) {
            now = Calendar.getInstance()
            delay(1_000)
        }
    }

    fun submit() {
        if (input.isEmpty()) return
        if (input.toInt() == problem.answer) {
            solved++
            if (solved >= problemCount) {
                onSolved()
                return
            }
            feedback = "정답! 다음 문제"
        } else {
            feedback = "틀렸어요. 새 문제예요"
        }
        problem = MathProblem.random()
        input = ""
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val hour = now.get(Calendar.HOUR_OF_DAY)
            Text(
                "${AlarmFormat.amPm(hour)} ${AlarmFormat.clockText(hour, now.get(Calendar.MINUTE))}",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(top = 16.dp),
            )
            Spacer(Modifier.height(24.dp))
            Text(
                "문제 ${solved + 1} / $problemCount",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LinearProgressIndicator(
                progress = { solved / problemCount.toFloat() },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )

            Spacer(Modifier.weight(1f))
            Text(
                "${problem.question} = ?",
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier.fillMaxWidth().height(64.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    input.ifEmpty { "_" },
                    fontSize = 40.sp,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                feedback ?: " ",
                style = MaterialTheme.typography.bodyLarge,
                color = if (feedback?.startsWith("틀") == true) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                },
            )
            Spacer(Modifier.weight(1f))

            NumberPad(
                onDigit = { digit ->
                    if (input.length < MAX_INPUT_LENGTH) input += digit
                    feedback = null
                },
                onDelete = { input = input.dropLast(1) },
                onSubmit = ::submit,
            )
        }
    }
}

@Composable
private fun NumberPad(onDigit: (Char) -> Unit, onDelete: () -> Unit, onSubmit: () -> Unit) {
    val rows = listOf("123", "456", "789")
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { digit -> PadKey(digit.toString()) { onDigit(digit) } }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PadKey("⌫", onClick = onDelete)
            PadKey("0") { onDigit('0') }
            Button(
                onClick = onSubmit,
                modifier = Modifier.weight(1f).aspectRatio(1.6f),
                colors = ButtonDefaults.buttonColors(),
            ) { Text("확인", fontSize = 20.sp) }
        }
    }
}

@Composable
private fun RowScope.PadKey(label: String, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier.weight(1f).aspectRatio(1.6f),
    ) { Text(label, fontSize = 24.sp) }
}
