package com.olegdanko.device_side

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.LinearLayout
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.olegdanko.device_side.databinding.ActivityControllerBinding
import okhttp3.internal.notify
import okhttp3.internal.wait
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class Vec2(var x: Float, var y: Float) {}

interface IEvent {}
class MovementEvent(private var coords: Vec2) : IEvent {
    fun add(event: MovementEvent) {
        coords.x += event.coords.x
        coords.y += event.coords.y
    }

    override fun toString(): String {
        return "mouse_mv ${coords.x} ${coords.y}"
    }
}
class MouseButtonEvent(private val name: String, private val isPressed: Boolean) : IEvent {
    override fun toString(): String {
        return "${if(isPressed) "press" else "release"} $name"
    }
}
class MouseEndEvent : IEvent {
    override fun toString(): String {
        return "mouse_end"
    }
}

class InputSender(var connectionProvider: ConnectionProvider) {
    private var eventQueue = ArrayDeque<IEvent>()
    private var lastEvent : IEvent? = null
    private var lastMovementEvent : MovementEvent? = null

    private var running: Boolean = true
    private var lock = Any()
    private var senderThread = Thread {
        while (true) {
            synchronized(lock) {
                if(!running) {
                    return@Thread
                }
                if(!eventQueue.isEmpty()) {
                    return@synchronized
                }
                lock.wait()
            }
            Thread.sleep(8)

            val eQueue = takeEvents()
            var message = ""
            while(!eQueue.isEmpty()) {
                message += eQueue.first.toString()
                message += " "
                eQueue.pop()
            }
            connectionProvider.send(message)
        }
    }

    init {
        senderThread.start()
    }

    fun stop() {
        synchronized(lock) {
            running = false
            lock.notify()
        }
        senderThread.join()
    }

    private fun takeEvents() : ArrayDeque<IEvent> {
        synchronized(lock) {
            val q = eventQueue;
            eventQueue = ArrayDeque<IEvent>()
            lastEvent = null
            return q
        }
    }
    private fun addEvent(event: IEvent) {
        synchronized(lock) {
            eventQueue.add(event)
            lastEvent = event
            lock.notify()
        }
    }

    fun move(x: Float, y: Float) {
        val movementEvent = MovementEvent(Vec2(x, y))
        synchronized(lock) {
            if (lastEvent == lastMovementEvent) {
                lastMovementEvent?.let { e -> e.add(movementEvent); return }
            }
        }
        addEvent(movementEvent)
        lastMovementEvent = movementEvent
    }
    fun moveDone() {
        addEvent(MouseEndEvent())
    }
    fun press(btn: String) {
        addEvent(MouseButtonEvent(btn, true))
    }
    fun release(btn: String) {
        addEvent(MouseButtonEvent(btn, false))
    }
}

class PositionTracker(var coords: Vec2 = Vec2(0.0f, 0.0f)) {

    fun set(newCoords: Vec2) {
        coords = newCoords
    }
    fun set(x: Float, y :Float) {
        set(Vec2(x, y))
    }
    fun move(x: Float, y :Float) : Vec2 {
        val delta = Vec2(x - coords.x, y - coords.y)
        set(x, y)
        return delta
    }
    fun move(newCoords: Vec2) : Vec2 {
        return move(newCoords.x, newCoords.y)
    }
}

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
class ControllerActivity : AppCompatActivity() {
    private var mVelocityTracker: VelocityTracker? = null
    private var mPositionTracker = PositionTracker()

    private lateinit var binding: ActivityControllerBinding
    private lateinit var settingsButton: Button
    private lateinit var fullscreenContentControls: LinearLayout
    private val hideHandler = Handler(Looper.myLooper()!!)

    private lateinit var connectionProvider: ConnectionProvider
    private lateinit var inputSender: InputSender
    fun set_click_handling(button: Button, name: String) {
        button.setOnTouchListener{ _, event ->
            when (event?.action) {
                MotionEvent.ACTION_DOWN -> inputSender.press(name)
                MotionEvent.ACTION_UP -> inputSender.release(name)
            }
            true
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        SingletonSharepoint.getInstance().takeConnectionProvider()?.let {provider ->
            connectionProvider = provider
        } ?: run {
            finish()
            return
        }

        inputSender = InputSender(connectionProvider)

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
            // TODO: remove touch slop
            val getCoords = {
                val pointerIndex = event.findPointerIndex(
                    event.getPointerId(event.actionIndex)
                )
                Vec2(event.getX(pointerIndex), event.getY(pointerIndex))
            }
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    mPositionTracker.set(getCoords())
                }
                MotionEvent.ACTION_MOVE -> {
                    val delta = mPositionTracker.move(getCoords())
                    inputSender.move(delta.x, delta.y)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    inputSender.moveDone()
                }
            }
            true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        inputSender.stop()
    }
}