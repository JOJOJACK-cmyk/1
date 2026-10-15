package com.jojojack.mathalarm.ui

import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.activity.result.contract.ActivityResultContract

/** 알람음 이름. null이면 기본 알람음. */
fun ringtoneTitle(context: Context, uri: String?): String {
    if (uri == null) return "기본 알람음"
    return runCatching { RingtoneManager.getRingtone(context, Uri.parse(uri))?.getTitle(context) }
        .getOrNull()
        ?: "알 수 없는 알람음"
}

/**
 * 시스템 알람음 선택 화면을 연다.
 * 입력: 현재 알람음 URI(null = 기본). 결과: 선택한 URI, 취소하면 [Cancelled].
 */
class PickRingtone : ActivityResultContract<String?, PickRingtone.Result>() {
    sealed interface Result
    data class Picked(val uri: String?) : Result
    data object Cancelled : Result

    override fun createIntent(context: Context, input: String?): Intent =
        Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "알람음 선택")
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
            putExtra(
                RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
            )
            putExtra(
                RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                input?.let(Uri::parse) ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
            )
        }

    override fun parseResult(resultCode: Int, intent: Intent?): Result {
        if (resultCode != android.app.Activity.RESULT_OK || intent == null) return Cancelled
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        }
        // "기본값"을 고르면 기본 알람음 URI가 오므로 null(기본)로 저장한다.
        return if (uri == null || RingtoneManager.isDefault(uri)) Picked(null) else Picked(uri.toString())
    }
}
