package com.antest1.gotobrowser.Browser

import android.content.Context
import android.widget.FrameLayout
import kotlin.math.roundToInt

class GameAreaLayout(context: Context) : FrameLayout(context) {
    // Space kept between the game and the split-screen divider, on the divider's side only.
    private var dividerMarginTopDp = 0
    private var dividerMarginBottomDp = 0

    fun setDividerMargin(topDp: Int, bottomDp: Int) {
        if (dividerMarginTopDp != topDp || dividerMarginBottomDp != bottomDp) {
            dividerMarginTopDp = topDp
            dividerMarginBottomDp = bottomDp
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

    private fun dpToPx(dp: Int) = (dp * resources.displayMetrics.density).roundToInt()

    // Padding is set from the display-cutout insets in BrowserActivity, so the 1200:720 fit
    // is computed against the cutout-free area only. The divider margin is reserved the same
    // way, so the game shrinks to keep its ratio: the WebView scales the page to its width,
    // and a WebView shorter than 1200:720 would cut the game off.
    private fun gameRect(width: Int, height: Int) = GameGeometry.fit(
        width.toFloat(), height.toFloat(), verticalBias,
        paddingLeft.toFloat(),
        (paddingTop + dpToPx(dividerMarginTopDp)).toFloat(),
        paddingRight.toFloat(),
        (paddingBottom + dpToPx(dividerMarginBottomDp)).toFloat()
    )

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
            MeasureSpec.makeMeasureSpec(rect.height.roundToInt(), MeasureSpec.EXACTLY)
        )
        setMeasuredDimension(containerWidth, containerHeight)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val child = getChildAt(0) ?: return
        val rect = gameRect(width, height)
        val childLeft = rect.left.roundToInt()
        val childTop = rect.top.roundToInt()
        child.layout(childLeft, childTop, childLeft + child.measuredWidth, childTop + child.measuredHeight)
    }
}
