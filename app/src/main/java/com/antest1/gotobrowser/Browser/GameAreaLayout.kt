package com.antest1.gotobrowser.Browser

import android.content.Context
import android.widget.FrameLayout
import kotlin.math.roundToInt

class GameAreaLayout(context: Context) : FrameLayout(context) {
    var marginDp: Int = 0
        set(value) {
            if (field != value) {
                field = value
                requestLayout()
            }
        }

    /** See [GameGeometry.fit]; the overlays lined up with the game get the same value. */
    var verticalBias: Float = GameGeometry.BIAS_CENTER
        set(value) {
            if (field != value) {
                field = value
                requestLayout()
            }
        }

    // Padding is set from the display-cutout insets in BrowserActivity,
    // so the 1200:720 fit is computed against the cutout-free area only.
    private fun gameRect(width: Int, height: Int) = GameGeometry.fit(
        width.toFloat(), height.toFloat(), verticalBias,
        paddingLeft.toFloat(), paddingTop.toFloat(), paddingRight.toFloat(), paddingBottom.toFloat()
    )

    private val marginPx get() = (marginDp * resources.displayMetrics.density).roundToInt()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val containerWidth = MeasureSpec.getSize(widthMeasureSpec)
        val containerHeight = MeasureSpec.getSize(heightMeasureSpec)
        if (containerWidth <= 0 || containerHeight <= 0) {
            setMeasuredDimension(containerWidth, containerHeight)
            return
        }

        val rect = gameRect(containerWidth, containerHeight)
        getChildAt(0)?.measure(
            MeasureSpec.makeMeasureSpec(rect.width.roundToInt(), MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec((rect.height.roundToInt() - marginPx * 2).coerceAtLeast(1), MeasureSpec.EXACTLY)
        )
        setMeasuredDimension(containerWidth, containerHeight)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val child = getChildAt(0) ?: return
        val rect = gameRect(width, height)
        // The multi-window margin shrinks the game by the same amount at the top and the
        // bottom, so the divider keeps its distance whichever way the game is aligned.
        val childLeft = rect.left.roundToInt()
        val childTop = rect.top.roundToInt() + marginPx
        child.layout(childLeft, childTop, childLeft + child.measuredWidth, childTop + child.measuredHeight)
    }
}
