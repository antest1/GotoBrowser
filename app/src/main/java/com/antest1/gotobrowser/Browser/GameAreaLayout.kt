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

        val ratio = 1200f / 720f
        val width: Int
        val height: Int
        if (containerWidth.toFloat() / containerHeight > ratio) {
            height = containerHeight
            width = (containerHeight * ratio).roundToInt()
        } else {
            width = containerWidth
            height = (containerWidth / ratio).roundToInt()
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
        val childLeft = (width - child.measuredWidth) / 2
        val childTop = (height - child.measuredHeight) / 2
        child.layout(childLeft, childTop, childLeft + child.measuredWidth, childTop + child.measuredHeight)
    }
}
