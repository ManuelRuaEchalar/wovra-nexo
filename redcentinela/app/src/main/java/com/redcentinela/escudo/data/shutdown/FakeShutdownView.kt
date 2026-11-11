package com.redcentinela.escudo.data.shutdown

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView

/**
 * La pantalla del apagado falso: muestra "Apagando…" un par de segundos y
 * queda en negro, como si el equipo se hubiera apagado. Sigue encendido y
 * registrando. El dueño sale tocando [EXIT_TAPS] veces seguidas.
 */
@SuppressLint("ViewConstructor")
class FakeShutdownView(
    context: Context,
    private val onDark: () -> Unit,
    private val onExit: () -> Unit
) : FrameLayout(context) {

    private val handler = Handler(Looper.getMainLooper())
    private var taps = 0
    private var firstTapAt = 0L

    init {
        setBackgroundColor(Color.BLACK)
        isClickable = true
        isFocusable = true
        val box = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            addView(ProgressBar(context).apply {
                indeterminateTintList = android.content.res.ColorStateList.valueOf(Color.WHITE)
            })
            addView(TextView(context).apply {
                text = "Apagando…"
                setTextColor(Color.WHITE)
                textSize = 18f
                setPadding(0, 48, 0, 0)
            })
        }
        addView(box, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER))
        handler.postDelayed({
            removeView(box)
            onDark()
        }, SHUTDOWN_ANIMATION_MS)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val now = System.currentTimeMillis()
            if (now - firstTapAt > TAP_WINDOW_MS) {
                taps = 0
                firstTapAt = now
            }
            if (++taps >= EXIT_TAPS) onExit()
        }
        return true
    }

    override fun onDetachedFromWindow() {
        handler.removeCallbacksAndMessages(null)
        super.onDetachedFromWindow()
    }

    companion object {
        const val EXIT_TAPS = 5
        private const val TAP_WINDOW_MS = 3_000L
        private const val SHUTDOWN_ANIMATION_MS = 2_600L
    }
}
