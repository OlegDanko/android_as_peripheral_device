package com.olegdanko.device_side

interface ConnectionProvider {
    fun setMessageCallback(callback: (String) -> Unit)
    fun setClosedCallback(callback: () -> Unit) : Boolean
    fun send(msg: String): Boolean
}
