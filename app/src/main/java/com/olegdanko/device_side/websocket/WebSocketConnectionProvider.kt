package com.olegdanko.device_side.websocket

import com.olegdanko.device_side.ConnectionProvider
import okhttp3.*
import okhttp3.internal.notify
import okhttp3.internal.wait

class WebSocketConnectionProvider(private var client: OkHttpClient,
                                  private val request: Request) : WebSocketListener(), ConnectionProvider {
    enum class State { OPENING, OPENED, FAILED, CLOSED }

    private var state = State.OPENING
    private lateinit var ws: WebSocket

    private var msgCallback : ((String) -> Unit)? = null
    private var closedCallback : (() -> Unit)? = null

    @Synchronized
    private fun awaitConnection() : State {
        if(state != State.OPENING)
            return state
        wait()
        return state
    }

    override fun connect(): Boolean {
        if(state == State.OPENED) return true
        state = State.OPENING
        ws = client.newWebSocket(request, this)
        return when(awaitConnection()) {
            State.FAILED -> false
            State.CLOSED -> false
            else -> true
        }
    }
    override fun connected() : Boolean {
        return state == State.OPENED
    }

    override fun send(msg: String): Boolean {
        if(state != State.OPENED)
            return false
        if(ws.send(msg)) {
            return true
        }
        state = State.FAILED
        return false
    }

    @Synchronized
    override fun setMessageCallback(callback: (String) -> Unit) {
        msgCallback = callback
    }

    @Synchronized
    override fun setClosedCallback(callback: () -> Unit) {
        closedCallback = callback
    }

    @Synchronized
    override fun onOpen(webSocket: WebSocket, response: Response) {
        println("WebSocket opened")
        state = State.OPENED
        notify()
    }

    @Synchronized
    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        println("WebSocket failed: ${t.message}")
        state = State.FAILED
        notify()
    }

    @Synchronized
    override fun onMessage(webSocket: WebSocket, text: String) {
        msgCallback?.invoke(text)
    }

    @Synchronized
    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
//    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        state = State.CLOSED
        closedCallback?.invoke()
        notify()
    }
}
