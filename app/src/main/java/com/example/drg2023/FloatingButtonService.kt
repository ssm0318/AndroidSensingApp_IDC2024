package com.example.drg2023

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Application
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.*
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.google.android.material.internal.ViewUtils.dpToPx
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class FloatingButtonService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var floatingButton: ImageView
    private lateinit var params: WindowManager.LayoutParams
    private lateinit var subButtons: Array<ImageView>
    private val lifecycleListener = FloatingButtonLifecycleListener()
    private var areSubButtonsVisible = false

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        floatingButton = createFloatingButton()

        val emotions = arrayOf("Excited", "Calm", "Sad", "Frustrated")
        val imageIds = arrayOf(R.drawable.excited, R.drawable.calm, R.drawable.sad, R.drawable.frustrated)
        subButtons = Array(4) { i -> createSubButton(emotions[i], imageIds[i]) }

        addToWindowManager()

        lifecycleListener.setFloatingButtonService(this)
        (application as Application).registerActivityLifecycleCallbacks(lifecycleListener)
    }

    @SuppressLint("AppCompatCustomView")
    class CustomImageView(context: Context, attrs: AttributeSet? = null) : ImageView(context, attrs) {
        private val sizeInDp = 50
        private val sizeInPx = (sizeInDp * resources.displayMetrics.density).toInt()

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            val width = MeasureSpec.makeMeasureSpec(sizeInPx, MeasureSpec.EXACTLY)
            val height = MeasureSpec.makeMeasureSpec(sizeInPx, MeasureSpec.EXACTLY)
            super.onMeasure(width, height)
        }
    }

    private fun createSubButton(emotion: String, drawableResId: Int): ImageView {
        val button = CustomImageView(this)
        button.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        button.setImageDrawable(ContextCompat.getDrawable(this, drawableResId))
        button.visibility = View.GONE

        button.setOnClickListener {
            openAppInForeground()

            val intent = Intent("com.example.drg2023.EMOTION_SELECTED")
            intent.putExtra("emotion", emotion)

            Handler(Looper.getMainLooper()).postDelayed({
                sendBroadcast(intent)
            }, 500)

            toggleSubButtons()
        }

        return button
    }

    private fun createFloatingButton(): ImageView {
        val button = ImageView(this)
        button.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        button.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.circular_logo))
        button.setOnTouchListener(floatingButtonTouchListener)
        return button
    }

    private fun addToWindowManager() {
        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.START
        params.x = 0
        params.y = 100

        windowManager.addView(floatingButton, params)

        subButtons.forEach { subButton ->
            windowManager.addView(subButton, params)
        }
    }

    private val floatingButtonTouchListener = object : View.OnTouchListener {
        private var initialX: Int = 0
        private var initialY: Int = 0
        private var initialTouchX: Float = 0f
        private var initialTouchY: Float = 0f
        private var isMoved: Boolean = false
        private val movementThreshold: Int = 100

        override fun onTouch(v: View, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isMoved = false
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = (event.rawX - initialTouchX).toInt()
                    val deltaY = (event.rawY - initialTouchY).toInt()
                    if (abs(deltaX) > movementThreshold || abs(deltaY) > movementThreshold) {
                        isMoved = true
                    }
                    params.x = initialX + deltaX
                    params.y = initialY + deltaY
                    windowManager.updateViewLayout(v, params)
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isMoved) {
                        animateButtonClick()
                        toggleSubButtons()
                    } else {
                        resetButtonSize()
                        moveSubButtons()
                    }
                    return true
                }
            }
            return false
        }
    }
    private fun toggleSubButtons() {
        areSubButtonsVisible = subButtons.firstOrNull { it.visibility == View.VISIBLE } != null

        if (areSubButtonsVisible) {
            subButtons.forEach { it.visibility = View.GONE }
        } else {
            moveSubButtons()
            subButtons.forEach { it.visibility = View.VISIBLE }
        }

        areSubButtonsVisible = !areSubButtonsVisible
    }

    private fun getDisplayMetrics(): DisplayMetrics {
        val displayMetrics = DisplayMetrics()
        val display = windowManager.defaultDisplay
        display.getMetrics(displayMetrics)
        return displayMetrics
    }

    private fun moveSubButtons() {
        val displayMetrics = getDisplayMetrics()
        val buttonSpacing = 180
        val totalWidth = buttonSpacing * subButtons.size + floatingButton.width
        val totalHeight = buttonSpacing * subButtons.size + floatingButton.height

        val screenCenterX = displayMetrics.widthPixels / 2
        val screenCenterY = displayMetrics.heightPixels / 2

        val placeButtonsVertically = if (params.x < screenCenterX) {
            params.x + totalWidth > displayMetrics.widthPixels
        } else {
            params.x - totalWidth < 0
        }

        for (i in subButtons.indices) {
            val subParams = WindowManager.LayoutParams(params.width, params.height, params.type, params.flags, params.format).apply {
                gravity = params.gravity
                x = params.x
                y = params.y
            }

            if (placeButtonsVertically) {
                val direction = if (params.y < screenCenterY) {
                    // if button is in the upper half of the screen
                    if (params.y + totalHeight > displayMetrics.heightPixels) -1 else 1
                } else {
                    // if button is in the lower half of the screen
                    if (params.y - totalHeight < 0) 1 else -1
                }
                subParams.y = params.y + direction * (buttonSpacing * (i + 1))
            } else {
                val direction = if (params.x < screenCenterX) 1 else -1
                subParams.x = params.x + direction * (buttonSpacing * (i + 1))
            }
            windowManager.updateViewLayout(subButtons[i], subParams)
        }
    }

    private fun animateButtonClick() {
        val buttonScaleDownAnimator = ValueAnimator.ofFloat(1f, 0.9f).apply {
            duration = 100
            addUpdateListener { animation ->
                val scaleValue = animation.animatedValue as Float
                floatingButton.scaleX = scaleValue
                floatingButton.scaleY = scaleValue
            }
        }

        buttonScaleDownAnimator.start()
    }

    fun resetButtonSize() {
        floatingButton.scaleX = 1f
        floatingButton.scaleY = 1f
    }

    private fun openAppInForeground() {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        windowManager.removeView(floatingButton)
        subButtons.forEach { windowManager.removeView(it) } // Remove the subButtons from the window manager
    }
}
