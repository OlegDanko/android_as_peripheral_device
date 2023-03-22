package com.olegdanko.device_side

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
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
    private var mVelocityTracker: VelocityTracker? = null

    private lateinit var binding: ActivityControllerBinding
    private lateinit var settingsButton: Button
    private lateinit var fullscreenContentControls: LinearLayout
    private val hideHandler = Handler(Looper.myLooper()!!)

    private lateinit var connectionProvider: ConnectionProvider

    fun set_click_handling(button: Button, name: String) {
        button.setOnTouchListener{ _, event ->
            when (event?.action) {
                MotionEvent.ACTION_DOWN -> connectionProvider.send("press ${name}")
                MotionEvent.ACTION_UP -> connectionProvider.send("release ${name}")
            }
            true
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
        } else {
            window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        binding = ActivityControllerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Set up the user interaction to manually show or hide the system UI.
        settingsButton = binding.btnControllerSettings
        settingsButton.setOnClickListener { connectionProvider.send("settings is pressed") }

        set_click_handling(binding.lmb, "lmb")
        set_click_handling(binding.mmb, "mmb")
        set_click_handling(binding.rmb, "rmb")

        binding.btnMouseMovement.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    mVelocityTracker?.clear()
                    mVelocityTracker = mVelocityTracker ?: VelocityTracker.obtain()
                    mVelocityTracker?.addMovement(event)
                }
                MotionEvent.ACTION_MOVE -> {
                    mVelocityTracker?.apply {
                        val pointerId: Int = event.getPointerId(event.actionIndex)
                        addMovement(event)
                        computeCurrentVelocity(1000)
                        var vX = getXVelocity(pointerId)
                        var vY = getYVelocity(pointerId)
                        connectionProvider.send("mouse_mv ${vX} ${vY}")
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    // Return a VelocityTracker object back to be re-used by others.
                    mVelocityTracker?.recycle()
                    mVelocityTracker = null
                    connectionProvider.send("mouse_end")
                }
            }
            true
        }
    }

//    override fun onTouchEvent(event: MotionEvent): Boolean {
//        when (event.actionMasked) {
//            MotionEvent.ACTION_DOWN -> {
//                // Reset the velocity tracker back to its initial state.
//                mVelocityTracker?.clear()
//                // If necessary retrieve a new VelocityTracker object to watch the
//                // velocity of a motion.
//                mVelocityTracker = mVelocityTracker ?: VelocityTracker.obtain()
//                // Add a user's movement to the tracker.
//                mVelocityTracker?.addMovement(event)
//            }
//            MotionEvent.ACTION_MOVE -> {
//                mVelocityTracker?.apply {
//                    val pointerId: Int = event.getPointerId(event.actionIndex)
//                    addMovement(event)
//                    // When you want to determine the velocity, call
//                    // computeCurrentVelocity(). Then call getXVelocity()
//                    // and getYVelocity() to retrieve the velocity for each pointer ID.
//                    computeCurrentVelocity(1000)
//                    // Log velocity of pixels per second
//                    // Best practice to use VelocityTrackerCompat where possible.
//                    Log.d("", "velocity: { ${getXVelocity(pointerId)}; ${getYVelocity(pointerId)}}")
//                    connectionProvider.send("mouse_mv ${getXVelocity(pointerId)} ${getYVelocity(pointerId)}")
//                }
//            }
//            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
//                // Return a VelocityTracker object back to be re-used by others.
//                mVelocityTracker?.recycle()
//                mVelocityTracker = null
//
//                connectionProvider.send("mouse_end")
//            }
//        }
//        return true
//    }
}