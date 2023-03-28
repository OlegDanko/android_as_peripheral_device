package com.olegdanko.device_side

interface ConnectionProvider {
    fun connect() : Boolean
    fun connected() : Boolean
    fun setMessageCallback(callback: (String) -> Unit)
    fun setClosedCallback(callback: () -> Unit)
    fun send(msg: String): Boolean
}
