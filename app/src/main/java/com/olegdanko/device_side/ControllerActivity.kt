package com.olegdanko.device_side

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Window
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.olegdanko.device_side.databinding.ActivityControllerBinding


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
class ControllerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityControllerBinding
    private lateinit var settingsButton: Button
    private lateinit var fullscreenContentControls: LinearLayout
    private val hideHandler = Handler(Looper.myLooper()!!)

    private lateinit var connectionProvider: ConnectionProvider

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        SingletonSharepoint.getInstance().takeConnectionProvider()?.let {provider ->
            connectionProvider = provider
        } ?: run {
            finish()
            return
        }

        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowInsetsController =
                WindowCompat.getInsetsController(window, window.decorView)
            windowInsetsController?.let {
                it.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
            windowInsetsController?.let{it.hide(WindowInsetsCompat.Type.systemBars())}

//
//            // Add a listener to update the behavior of the toggle fullscreen button when
//            // the system bars are hidden or revealed.
//            window.decorView.setOnApplyWindowInsetsListener { view, windowInsets ->
//                // You can hide the caption bar even when the other system bars are visible.
//                // To account for this, explicitly check the visibility of navigationBars()
//                // and statusBars() rather than checking the visibility of systemBars().
//                if (windowInsets.isVisible(WindowInsetsCompat.Type.navigationBars())
//                    || windowInsets.isVisible(WindowInsetsCompat.Type.statusBars())
//                ) {
//                    binding.btnControllerSettings.setOnClickListener {
//                        // Hide both the status bar and the navigation bar.
//                        windowInsetsController?.let{it.hide(WindowInsetsCompat.Type.systemBars())}
//                    }
//                } else {
//                    binding.btnControllerSettings.setOnClickListener {
//                        // Show both the status bar and the navigation bar.
//                        windowInsetsController?.let{it.show(WindowInsetsCompat.Type.systemBars())}
//                    }
//                }
//                view.onApplyWindowInsets(windowInsets)
//            }
        } else {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        super.onCreate(savedInstanceState)
        binding = ActivityControllerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Set up the user interaction to manually show or hide the system UI.
        settingsButton = binding.btnControllerSettings
        settingsButton.setOnClickListener { connectionProvider.send("settings is pressed") }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
//        delayedHide(100)
    }

}