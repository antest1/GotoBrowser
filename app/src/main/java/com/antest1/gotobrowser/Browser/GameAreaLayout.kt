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

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val containerWidth = MeasureSpec.getSize(widthMeasureSpec)
        val containerHeight = MeasureSpec.getSize(heightMeasureSpec)
        if (containerWidth <= 0 || containerHeight <= 0) {
            setMeasuredDimension(containerWidth, containerHeight)
            return
        }

        // Padding is set from the display-cutout insets in BrowserActivity,
        // so the 1200:720 fit is computed against the cutout-free area only.
        val availableWidth = (containerWidth - paddingLeft - paddingRight).coerceAtLeast(1)
        val availableHeight = (containerHeight - paddingTop - paddingBottom).coerceAtLeast(1)

        val ratio = 1200f / 720f
        val width: Int
        val height: Int
        if (availableWidth.toFloat() / availableHeight > ratio) {
            height = availableHeight
            width = (availableHeight * ratio).roundToInt()
        } else {
            width = availableWidth
            height = (availableWidth / ratio).roundToInt()
        }

        val margin = (marginDp * resources.displayMetrics.density).roundToInt()
        getChildAt(0)?.measure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec((height - margin * 2).coerceAtLeast(1), MeasureSpec.EXACTLY)
        )
        setMeasuredDimension(containerWidth, containerHeight)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val child = getChildAt(0) ?: return
        val availableWidth = width - paddingLeft - paddingRight
        val availableHeight = height - paddingTop - paddingBottom
        val childLeft = paddingLeft + (availableWidth - child.measuredWidth) / 2
        val childTop = paddingTop + (availableHeight - child.measuredHeight) / 2
        child.layout(childLeft, childTop, childLeft + child.measuredWidth, childTop + child.measuredHeight)
    }
}