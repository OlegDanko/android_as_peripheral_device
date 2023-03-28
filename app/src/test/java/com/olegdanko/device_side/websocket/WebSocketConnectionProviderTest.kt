package com.olegdanko.device_side.websocket

import org.junit.Assert.*
import okhttp3.*
import org.junit.Test
import org.mockito.Mockito.*
import java.util.concurrent.Executors

class WebSocketConnectionProviderTest {
    private fun dummyResponse(request: Request, code: Int): Response {
        return Response.Builder()
            .request(request)
            .code(code)
            .protocol(Protocol.HTTP_2)
            .message("dummy")
            .build()
    }

    @Test
    fun connectionNotifyBeforeReturnWsTest() {
        val clientMock = mock(OkHttpClient::class.java)
        val requestMock = mock(Request::class.java)
        val websocketMock = mock(WebSocket::class.java)

        val provider = WebSocketConnectionProvider(clientMock, requestMock)

        `when`(clientMock.newWebSocket(requestMock, provider)).then {
            provider.onOpen(websocketMock, dummyResponse(requestMock, 200))
            websocketMock
        }

        assertTrue(provider.connect())
        assertTrue(provider.connected())
    }

    @Test
    fun connectionReturnWsBeforeNotifyTest() {
        val clientMock = mock(OkHttpClient::class.java)
        val requestMock = mock(Request::class.java)
        val websocketMock = mock(WebSocket::class.java)

        val provider = WebSocketConnectionProvider(clientMock, requestMock)

        `when`(clientMock.newWebSocket(requestMock, provider)).then {
            Executors.newSingleThreadExecutor().execute {
                Thread.sleep(5)
                provider.onOpen(websocketMock, dummyResponse(requestMock, 0))
            }
            websocketMock
        }

        assertTrue(provider.connect())
        assertTrue(provider.connected())
    }

    @Test
    fun connectionFailsTest() {
        val clientMock = mock(OkHttpClient::class.java)
        val requestMock = mock(Request::class.java)
        val websocketMock = mock(WebSocket::class.java)

        val provider = WebSocketConnectionProvider(clientMock, requestMock)

        `when`(clientMock.newWebSocket(requestMock, provider)).then {
            provider.onFailure( websocketMock, Throwable(), dummyResponse(requestMock, 1014)
            )
            websocketMock
        }

        assertFalse(provider.connected())
        assertFalse(provider.connect())

        // Can connect afterwards
        `when`(clientMock.newWebSocket(requestMock, provider)).then {
            provider.onOpen(websocketMock, dummyResponse(requestMock, 200))
            websocketMock
        }

        assertTrue(provider.connect())
        assertTrue(provider.connected())
    }

    @Test
    fun connectionClosesImmediatelyTest() {
        val clientMock = mock(OkHttpClient::class.java)
        val requestMock = mock(Request::class.java)
        val websocketMock = mock(WebSocket::class.java)

        val provider = WebSocketConnectionProvider(clientMock, requestMock)

        `when`(clientMock.newWebSocket(requestMock, provider)).then {
            provider.onOpen(websocketMock, dummyResponse(requestMock, 0))
            provider.onClosed(websocketMock, 1000, "dummy")
            websocketMock
        }

        assertFalse(provider.connect())
        assertFalse(provider.connected())

        // Can connect afterwards
        `when`(clientMock.newWebSocket(requestMock, provider)).then {
            provider.onOpen(websocketMock, dummyResponse(requestMock, 200))
            websocketMock
        }

        assertTrue(provider.connect())
        assertTrue(provider.connected())
    }

    @Test
    fun onClosedTest() {
        val clientMock = mock(OkHttpClient::class.java)
        val requestMock = mock(Request::class.java)
        val websocketMock = mock(WebSocket::class.java)

        val provider = WebSocketConnectionProvider(clientMock, requestMock)

        `when`(clientMock.newWebSocket(requestMock, provider)).then {
            Executors.newSingleThreadExecutor().execute {
                Thread.sleep(5)
                provider.onOpen(websocketMock, dummyResponse(requestMock, 0))
            }
            websocketMock
        }

        provider.connect()
        provider.onClosed(websocketMock, 1000, "test")
        assertFalse(provider.connected())
    }

    @Test
    fun onClosedCallbackTest() {
        val clientMock = mock(OkHttpClient::class.java)
        val requestMock = mock(Request::class.java)
        val websocketMock = mock(WebSocket::class.java)

        val provider = WebSocketConnectionProvider(clientMock, requestMock)

        `when`(clientMock.newWebSocket(requestMock, provider)).then {
            Executors.newSingleThreadExecutor().execute {
                Thread.sleep(5)
                provider.onOpen(websocketMock, dummyResponse(requestMock, 0))
            }
            websocketMock
        }

        provider.connect()
        var closed = false
        provider.setClosedCallback { closed = true }
        provider.onClosed(websocketMock, 1000, "dummy")

        assertTrue(closed)
    }

    @Test
    fun onMessageTest() {
        val clientMock = mock(OkHttpClient::class.java)
        val requestMock = mock(Request::class.java)
        val websocketMock = mock(WebSocket::class.java)

        val provider = WebSocketConnectionProvider(clientMock, requestMock)

        provider.onMessage(websocketMock, "test_1")

        var message = ""
        provider.setMessageCallback {msg -> message = msg }
        provider.onMessage(websocketMock, "test_2")
        assertEquals(message, "test_2")
    }

    @Test
    fun sendTest() {
        val clientMock = mock(OkHttpClient::class.java)
        val requestMock = mock(Request::class.java)
        val websocketMock = mock(WebSocket::class.java)

        val provider = WebSocketConnectionProvider(clientMock, requestMock)

        `when`(clientMock.newWebSocket(requestMock, provider)).then {
            provider.onFailure(websocketMock, Throwable(), dummyResponse(requestMock, 1014))
            websocketMock
        }
        provider.connect()
        assertFalse(provider.send("msg_0"))

        `when`(clientMock.newWebSocket(requestMock, provider)).then {
            provider.onOpen(websocketMock, dummyResponse(requestMock, 0))
            websocketMock
        }
        provider.connect()

        `when`(websocketMock.send("msg_1")).thenReturn(true)
        assertTrue(provider.send("msg_1"))

        provider.onClosed(websocketMock, 1000, "closed")
        assertFalse(provider.send("msg_2"))

        verify(websocketMock, never()).send("msg_0")
        verify(websocketMock, never()).send("msg_2")
        verify(websocketMock, times(1)).send("msg_1")
    }

    @Test
    fun sendFailedShouldDisconnectTest() {
        val clientMock = mock(OkHttpClient::class.java)
        val requestMock = mock(Request::class.java)
        val websocketMock = mock(WebSocket::class.java)

        val provider = WebSocketConnectionProvider(clientMock, requestMock)

        `when`(clientMock.newWebSocket(requestMock, provider)).then {
            provider.onOpen(websocketMock, dummyResponse(requestMock, 0))
            websocketMock
        }
        provider.connect()

        `when`(websocketMock.send("msg")).thenReturn(false)
        assertFalse(provider.send("msg"))
        verify(websocketMock, times(1)).send("msg")
        assertFalse(provider.connected())
    }
}