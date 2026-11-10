package com.akergez.superreader2000.shared.sync

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Вход через сервер на Android: страница сервера возвращает в приложение по схеме
 * [RETURN_TO] (intent-filter у MainActivity), активити отдаёт URI в [ServerConnectReturns].
 */
class AndroidServerConnectHost(private val context: Context) : ServerConnectHost {
    override val returnTo: String = RETURN_TO

    override fun listen(): Boolean = true

    override fun open(url: String): Boolean = runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.isSuccess

    companion object {
        const val SCHEME = "superreader2000"
        const val HOST = "tresse-connect"
        const val RETURN_TO = "$SCHEME://$HOST"

        /** Это возврат со страницы сервера? Тогда его URI, иначе null. */
        fun returnedUri(data: Uri?): String? =
            data?.takeIf { it.scheme == SCHEME && it.host == HOST }?.toString()
    }
}
