package com.olegdanko.device_side

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.olegdanko.device_side.databinding.FragmentWifiChannelBinding
import com.olegdanko.device_side.websocket.WebSocketConnectionProvider
import okhttp3.*

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class WifiChannelFragment : Fragment() {
    private var ipPortStr = "192.168.1.104:33333"
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

    private fun showNotImplemented(str: String) {
        showSnackbar("$str is not implemented yet")
    }

    private fun showSnackbar(str: String) {
        val view: View = super.getView() ?: return
        Snackbar.make(view, str, Snackbar.LENGTH_SHORT).show()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnQr.setOnClickListener {
            showNotImplemented("Connection with QR");
        }
        binding.btnCode.setOnClickListener {
            startControlActivity("192.168.1.104:33333");
        }
        binding.btnIpAddr.setOnClickListener {
            val builder = AlertDialog.Builder(context)
            builder.setTitle("Enter ip address and port")
            val input = EditText(context)
            input.setText(ipPortStr)
            builder.setView(input)
            builder.setPositiveButton("OK") { dialog, _ ->
                ipPortStr = input.text.toString()
                dialog.dismiss()
                startControlActivity(ipPortStr)
            }
            builder.setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }
            builder.create().apply {
                setCancelable(true)
                setCanceledOnTouchOutside(true)
                show()
            }
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

    private fun startControlActivity(ipPort: String) {
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
        val request = Request.Builder().url("ws://$ipPort").build()
        val wsConnectionProvider = WebSocketConnectionProvider(client, request)

        showSnackbar("Connecting to websocket")

        if(!wsConnectionProvider.connect()) {
            showSnackbar("Failed to connect")
            return
        }
        showSnackbar("Connected successfully")

        SingletonSharepoint.getInstance().putConnectionProvider(wsConnectionProvider)
        startActivity(Intent(activity, ControllerActivity::class.java))
    }
}

