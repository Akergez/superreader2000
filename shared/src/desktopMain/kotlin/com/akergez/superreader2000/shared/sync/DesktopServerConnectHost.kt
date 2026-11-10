package com.akergez.superreader2000.shared.sync

import java.awt.Desktop
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.ServerSocket
import java.net.URI

/**
 * Вход через сервер на десктопе: вместо регистрации схемы URI в ОС (у каждой системы
 * свой механизм, у упакованных инсталляторов — свои ограничения) возвратным адресом
 * служит `http://localhost:PORT`, и браузер сам стучится сюда обычным GET. Сервер
 * должен разрешить этот адрес в своих `return_targets`.
 *
 * Порт свой, не как у Shardkeep (8765): оба приложения могут ждать возврата
 * одновременно.
 *
 * Сознательно на голом `ServerSocket`, а не `com.sun.net.httpserver.HttpServer`:
 * упакованный jlink-рантайм модуль jdk.httpserver может не включать.
 */
class DesktopServerConnectHost : ServerConnectHost {
    override val returnTo: String = BASE

    private var server: ServerSocket? = null

    @Synchronized
    override fun listen(): Boolean {
        stop()
        val listener = try {
            ServerSocket(PORT, 1, InetAddress.getByName("127.0.0.1")).apply {
                // Принимаем один возврат; дальше попытка протухает сама.
                soTimeout = SERVER_CONNECT_TTL_MS.toInt()
            }
        } catch (_: Exception) {
            return false
        }
        server = listener
        val thread = Thread {
            runCatching {
                listener.accept().use { connection ->
                    val requestLine = BufferedReader(InputStreamReader(connection.getInputStream())).readLine()
                    // Браузер шлёт origin-form (`GET /?… HTTP/1.1`); прокси может
                    // дать absolute-form — оба вида приводим к полному URI.
                    val target = requestLine?.split(' ')?.getOrNull(1).orEmpty()
                    val returned = if (target.startsWith("http")) target else BASE + target
                    val body = RESPONSE.toByteArray()
                    connection.getOutputStream().use { out ->
                        out.write(
                            (
                                "HTTP/1.1 200 OK\r\n" +
                                    "Content-Type: text/html; charset=utf-8\r\n" +
                                    "Content-Length: ${body.size}\r\n" +
                                    "Connection: close\r\n\r\n"
                                ).toByteArray(),
                        )
                        out.write(body)
                    }
                    ServerConnectReturns.deliver(returned)
                }
            }
            // Закрываем только свой сокет: новая попытка могла уже поднять следующий.
            release(listener)
        }
        thread.isDaemon = true
        thread.start()
        return true
    }

    override fun open(url: String): Boolean {
        val viaAwt = runCatching {
            check(Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE))
            Desktop.getDesktop().browse(URI(url))
        }.isSuccess
        // На части Linux-окружений AWT браузер не открывает — тогда через xdg-open.
        return viaAwt || runCatching { ProcessBuilder("xdg-open", url).start() }.isSuccess
    }

    @Synchronized
    private fun stop() {
        runCatching { server?.close() }
        server = null
    }

    @Synchronized
    private fun release(listener: ServerSocket) {
        runCatching { listener.close() }
        if (server === listener) server = null
    }

    private companion object {
        const val PORT = 8766
        const val BASE = "http://localhost:$PORT"

        val RESPONSE =
            "<!doctype html><html><head><meta charset=\"utf-8\"><title>SuperReader2000</title></head>" +
                "<body style=\"font-family:sans-serif;text-align:center;margin-top:4em\">" +
                "<p>Настройки синхронизации переданы в SuperReader2000.</p>" +
                "<p>Вернитесь в приложение — оно спросит подтверждение.</p>" +
                "<p style=\"color:gray\">Sync settings received — return to the app; you can close this tab.</p>" +
                "</body></html>"
    }
}
