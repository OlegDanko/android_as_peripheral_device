package com.olegdanko.device_side

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.olegdanko.device_side.databinding.FragmentWifiChannelBinding
import okhttp3.*
import okhttp3.internal.notify
import okhttp3.internal.wait



class WSListener(private var state: State, var msgCallback : ((String) -> Unit)?, var closedCallback : (() -> Unit)?) : WebSocketListener() {
    enum class State {
        OPENING, OPENED, FAILED, CLOSED
    }
    constructor() : this(State.OPENING, null, null) {}

    @Synchronized
    override fun onOpen(webSocket: WebSocket, response: Response) {
        println("WebSocket opened")
        state = State.OPENED
        notify()
    }

    @Synchronized
    override fun onMessage(webSocket: WebSocket, text: String) {
        msgCallback?.invoke(text)
    }

    @Synchronized
    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        state = State.CLOSED
        closedCallback?.invoke()
        notify()
    }

    @Synchronized
    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        println("WebSocket failed: ${t.message}")
        state = State.FAILED
        notify()
    }

    @Synchronized
    fun awaitConnection() : State {
        if(state != State.OPENING)
            return state
        wait()
        return state
    }
    @Synchronized
    fun setMessageCallback(cb: (String) -> Unit) {
        msgCallback = cb
    }
    @Synchronized
    fun setClosedCallback(cb: () -> Unit) : Boolean {
        closedCallback = cb
        return when(state) {
            State.FAILED, State.CLOSED -> false
            else -> true
        }
    }
}

class WebSocketProvider(var ws: WebSocket, var listener: WSListener) : ConnectionProvider {
    override fun send(msg: String): Boolean {
        return ws.send(msg)
    }

    override fun setMessageCallback(callback: (String) -> Unit) {
        listener.setMessageCallback(callback)
    }

    override fun setClosedCallback(callback: () -> Unit) : Boolean {
        return listener.setClosedCallback(callback)
    }
}

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class WifiChannelFragment : Fragment() {

    private var _binding: FragmentWifiChannelBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentWifiChannelBinding.inflate(inflater, container, false)
        return binding.root
    }

    fun showNotImplemented(str: String) {
        showSnackbar("$str is not implemented yet")
    }

    fun showSnackbar(str: String) {
        var view: View = super.getView() ?: return
        Snackbar.make(view, str, Snackbar.LENGTH_SHORT).show();
//            .setAction("Action", null).show()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnQr.setOnClickListener {
            showNotImplemented("Connection with QR");
        }
        binding.btnCode.setOnClickListener {
            startControlActivity();
        }
        binding.btnIpAddr.setOnClickListener {
            showNotImplemented("Connection with ip address");
        }
        binding.btnPrev.setOnClickListener {
            findNavController().navigate(R.id.action_wifi_to_channel_selection)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        showSnackbar("Checking permission result")
        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showSnackbar("Permission granted")
            } else {
                showSnackbar("Permission denied")
            }
        }
    }

    fun startControlActivity() {
        context?.let { ctx ->
            showSnackbar("Context exists")
            if (ContextCompat.checkSelfPermission(
                    ctx,
                    Manifest.permission.INTERNET
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                showSnackbar("Permission required")
                activity?.let {act ->
                    ActivityCompat.requestPermissions(
                        act,
                        arrayOf(Manifest.permission.INTERNET),
                        1
                    )
                }
            }
        }

        val client = OkHttpClient()
        val request = Request.Builder().url("ws://192.168.1.104:33333").build()
        val listener = WSListener()
        var websocket = client.newWebSocket(request, listener)

        showSnackbar("Connecting to websocket")

        if(when(listener.awaitConnection()) {
            WSListener.State.FAILED -> {
                showSnackbar("Failed to connect")
                true
            }
            WSListener.State.CLOSED -> {
                showSnackbar("Disconnected immediately")
                true
            }
            else -> false
        }) {
            return
        }
        showSnackbar("Connected successfully")

        SingletonSharepoint.getInstance().putConnectionProvider(
            WebSocketProvider(websocket, listener))
        startActivity(Intent(activity, ControllerActivity::class.java))
    }


}

