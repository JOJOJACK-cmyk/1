package com.jojojack.mathalarm.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jojojack.mathalarm.data.Alarm
import com.jojojack.mathalarm.data.AlarmFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditDialog(
    initial: Alarm,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (Alarm) -> Unit,
    onDelete: () -> Unit,
) {
    val timeState = rememberTimePickerState(
        initialHour = initial.hour,
        initialMinute = initial.minute,
        is24Hour = false,
    )
    var repeatDays by remember { mutableStateOf(initial.repeatDays) }
    var problemCount by remember { mutableIntStateOf(initial.problemCount) }
    var ringtoneUri by remember { mutableStateOf(initial.ringtoneUri) }
    val context = LocalContext.current
    val ringtoneName = remember(ringtoneUri) { ringtoneTitle(context, ringtoneUri) }
    val ringtonePicker = rememberLauncherForActivityResult(PickRingtone()) { result ->
        if (result is PickRingtone.Picked) ringtoneUri = result.uri
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.fillMaxWidth(0.92f),
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    if (isNew) "알람 추가" else "알람 수정",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                )

                TimePicker(state = timeState)

                SectionTitle("반복")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    AlarmFormat.WEEK_ORDER.forEach { day ->
                        val selected = day in repeatDays
                        FilterChip(
                            selected = selected,
                            onClick = { repeatDays = if (selected) repeatDays - day else repeatDays + day },
                            label = { Text(AlarmFormat.dayLabel(day)) },
                            modifier = Modifier.width(40.dp),
                        )
                    }
                }
                Text(
                    AlarmFormat.repeatText(repeatDays),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )

                SectionTitle("끄려면 풀어야 할 문제 수")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalIconButton(
                        onClick = { problemCount-- },
                        enabled = problemCount > Alarm.MIN_PROBLEM_COUNT,
                        shape = CircleShape,
                        modifier = Modifier.size(44.dp),
                    ) { Text("−", style = MaterialTheme.typography.titleLarge) }
                    Text(
                        "${problemCount}개",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(horizontal = 24.dp),
                    )
                    FilledTonalIconButton(
                        onClick = { problemCount++ },
                        enabled = problemCount < Alarm.MAX_PROBLEM_COUNT,
                        shape = CircleShape,
                        modifier = Modifier.size(44.dp),
                    ) { Text("+", style = MaterialTheme.typography.titleLarge) }
                }

                SectionTitle("알람음")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { ringtonePicker.launch(ringtoneUri) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        ringtoneName,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text("변경", color = MaterialTheme.colorScheme.primary)
                }

                Spacer(Modifier.padding(top = 16.dp))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    if (!isNew) {
                        TextButton(onClick = onDelete) {
                            Text("삭제", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("취소") }
                    TextButton(onClick = {
                        onSave(
                            initial.copy(
                                hour = timeState.hour,
                                minute = timeState.minute,
                                repeatDays = repeatDays,
                                problemCount = problemCount,
                                ringtoneUri = ringtoneUri,
                            ),
                        )
                    }) { Text("저장") }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
    )
}
