package com.olegdanko.device_side


class SingletonSharepoint private constructor() {
    var connectionProvider: ConnectionProvider? = null

    companion object {
        @Volatile
        private var INSTANCE: SingletonSharepoint? = null

        fun getInstance(): SingletonSharepoint {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SingletonSharepoint().also { INSTANCE = it }
            }
        }
    }

    @Synchronized
    fun putConnectionProvider(connectionProvider: ConnectionProvider)  {
        this.connectionProvider = connectionProvider
    }
    @Synchronized
    fun takeConnectionProvider() : ConnectionProvider? {
        return connectionProvider.also { connectionProvider = null }
    }
}