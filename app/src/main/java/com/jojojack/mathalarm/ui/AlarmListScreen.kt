package com.jojojack.mathalarm.ui

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.jojojack.mathalarm.alarm.AlarmScheduler
import com.jojojack.mathalarm.data.Alarm
import com.jojojack.mathalarm.data.AlarmFormat
import com.jojojack.mathalarm.data.AlarmRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmListScreen() {
    val context = LocalContext.current
    val repository = remember { AlarmRepository(context) }
    var alarms by remember { mutableStateOf(repository.getAll()) }
    // 권한 상태는 설정 화면에서 돌아올 때마다 다시 확인한다.
    var permissionCheck by remember { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf<Alarm?>(null) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        alarms = repository.getAll()
        permissionCheck++
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { permissionCheck++ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission(context)) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun saveAndSchedule(alarm: Alarm) {
        repository.save(alarm)
        if (alarm.enabled) {
            val triggerAt = AlarmScheduler.schedule(context, alarm)
            val message = if (triggerAt != null) {
                AlarmFormat.untilText(triggerAt - System.currentTimeMillis())
            } else {
                "정확한 알람 권한을 허용해 주세요"
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        } else {
            AlarmScheduler.cancel(context, alarm.id)
        }
        alarms = repository.getAll()
    }

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("산수 알람") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editing = Alarm(id = repository.nextId(), hour = 7, minute = 0)
            }) {
                Icon(Icons.Default.Add, contentDescription = "알람 추가")
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "permissions-$permissionCheck") {
                PermissionBanners(context, onRequestNotification = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                })
            }
            if (alarms.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(top = 64.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "+ 버튼을 눌러 알람을 추가하세요",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            items(alarms, key = { it.id }) { alarm ->
                AlarmCard(
                    alarm = alarm,
                    onClick = { editing = alarm },
                    onToggle = { enabled -> saveAndSchedule(alarm.copy(enabled = enabled)) },
                )
            }
        }
    }

    editing?.let { alarm ->
        AlarmEditDialog(
            initial = alarm,
            isNew = alarms.none { it.id == alarm.id },
            onDismiss = { editing = null },
            onSave = {
                saveAndSchedule(it.copy(enabled = true))
                editing = null
            },
            onDelete = {
                AlarmScheduler.cancel(context, alarm.id)
                repository.delete(alarm.id)
                alarms = repository.getAll()
                editing = null
            },
        )
    }
}

@Composable
private fun AlarmCard(alarm: Alarm, onClick: () -> Unit, onToggle: (Boolean) -> Unit) {
    val contentAlpha = if (alarm.enabled) 1f else 0.5f
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        AlarmFormat.amPm(alarm.hour),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        AlarmFormat.clockText(alarm.hour, alarm.minute),
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
                    )
                }
                Text(
                    "${AlarmFormat.repeatText(alarm.repeatDays)} · 문제 ${alarm.problemCount}개",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
                )
            }
            Switch(checked = alarm.enabled, onCheckedChange = onToggle)
        }
    }
}

/** 알람이 제대로 울리는 데 필요한 권한이 빠져 있으면 안내 카드를 보여준다. */
@Composable
private fun PermissionBanners(context: Context, onRequestNotification: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission(context)) {
            PermissionBanner(
                text = "알림 권한이 없으면 알람 화면이 표시되지 않아요.",
                action = "허용",
                onAction = onRequestNotification,
            )
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !AlarmScheduler.canScheduleExact(context)) {
            PermissionBanner(
                text = "정확한 시간에 울리려면 '알람 및 리마인더' 권한이 필요해요.",
                action = "설정",
                onAction = {
                    context.startActivity(
                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, packageUri(context)),
                    )
                },
            )
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            !context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
        ) {
            PermissionBanner(
                text = "잠금화면에서 알람 화면을 띄우려면 '전체 화면 알림' 권한이 필요해요.",
                action = "설정",
                onAction = {
                    context.startActivity(
                        Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, packageUri(context)),
                    )
                },
            )
        }
    }
}

@Composable
private fun PermissionBanner(text: String, action: String, onAction: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            TextButton(onClick = onAction) { Text(action) }
        }
    }
}

private fun hasNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

private fun packageUri(context: Context): Uri = Uri.parse("package:${context.packageName}")
