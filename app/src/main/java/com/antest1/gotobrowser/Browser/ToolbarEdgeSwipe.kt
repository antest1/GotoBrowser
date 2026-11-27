package com.antest1.gotobrowser.Browser

import android.annotation.SuppressLint
import android.content.Context
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.ViewConfiguration
import android.widget.FrameLayout
import kotlin.math.abs

/**
 * The swipe that reveals the hidden toolbar where the game leaves no margin to swipe from, so
 * that it has to start over the game. The toolbar says where it can start and moves the bar;
 * [ToolbarEdgeSwipeLayout] detects it. An overlay strip with pointer input, as used in the
 * margins, would also take the taps meant for the game.
 */
class ToolbarEdgeSwipe {
    /** Where the swipe can start, in the root layout's pixels, or null while it is off. */
    var strip: GameGeometry.Rect? = null

    /** The bar comes in from the left edge (landscape); otherwise from the bottom. */
    var fromLeft = true

    /** The finger moved this far toward the inside of the screen, in pixels. */
    var onDrag: (Float) -> Unit = {}

    /** The finger left at this speed toward the inside of the screen, in pixels per second. */
    var onDragEnd: (Float) -> Unit = {}
}

/**
 * Whether a finger that went down on the strip is swiping the toolbar in, after moving
 * [inward] (toward the inside of the screen) and [across]; null while it is within the touch
 * [slop]. Only a move that is mostly inward counts: anything else belongs to the game.
 */
fun revealSwipeStarts(inward: Float, across: Float, slop: Float): Boolean? =
    if (maxOf(abs(inward), abs(across)) <= slop) null else inward > abs(across)

/**
 * Root of the browser screen. Watches the touches on their way to the game and the overlays
 * for the [edgeSwipe] and takes over once one starts, as the edge of a drawer does. Taps and
 * other gestures on the strip go on to the game.
 */
class ToolbarEdgeSwipeLayout(context: Context, private val edgeSwipe: ToolbarEdgeSwipe) : FrameLayout(context) {
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop.toFloat()
    private var velocityTracker: VelocityTracker? = null
    private var pointerId = MotionEvent.INVALID_POINTER_ID

    /** A finger went down on the strip and has not moved past the touch slop yet. */
    private var watching = false
    private var dragging = false
    private var fromLeft = true
    private var downX = 0f
    private var downY = 0f

    /** Where the finger last was along the swipe, growing toward the inside of the screen. */
    private var lastInward = 0f

    private fun inward(x: Float, y: Float) = if (fromLeft) x else -y

    /** Ends whatever the current gesture was; a drag in progress settles at [velocity]. */
    private fun endGesture(velocity: Float = 0f) {
        val wasDragging = dragging
        watching = false
        dragging = false
        pointerId = MotionEvent.INVALID_POINTER_ID
        velocityTracker?.recycle()
        velocityTracker = null
        if (wasDragging) edgeSwipe.onDragEnd(velocity)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                endGesture()
                val strip = edgeSwipe.strip
                if (strip != null && ev.x >= strip.left && ev.x < strip.right && ev.y >= strip.top && ev.y < strip.bottom) {
                    watching = true
                    fromLeft = edgeSwipe.fromLeft
                    pointerId = ev.getPointerId(0)
                    downX = ev.x
                    downY = ev.y
                    velocityTracker = VelocityTracker.obtain().also { it.addMovement(ev) }
                }
            }
            MotionEvent.ACTION_MOVE -> if (watching) {
                val index = ev.findPointerIndex(pointerId)
                if (index < 0) {
                    endGesture()
                } else {
                    velocityTracker?.addMovement(ev)
                    val x = ev.getX(index)
                    val y = ev.getY(index)
                    val across = if (fromLeft) y - downY else x - downX
                    when (revealSwipeStarts(inward(x, y) - inward(downX, downY), across, touchSlop)) {
                        // The view under the finger gets a cancel; the rest comes to onTouchEvent.
                        true -> {
                            watching = false
                            dragging = true
                            lastInward = inward(x, y)
                        }
                        false -> endGesture()
                        null -> Unit
                    }
                }
            }
            // A second finger makes it a pinch, not a swipe in from the edge.
            MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                if (watching) endGesture()
        }
        return dragging
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (!dragging) return super.onTouchEvent(ev)
        velocityTracker?.addMovement(ev)
        when (ev.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                val index = ev.findPointerIndex(pointerId)
                if (index >= 0) {
                    val position = inward(ev.getX(index), ev.getY(index))
                    edgeSwipe.onDrag(position - lastInward)
                    lastInward = position
                }
            }
            MotionEvent.ACTION_UP -> endGesture(releaseVelocity())
            MotionEvent.ACTION_POINTER_UP ->
                if (ev.getPointerId(ev.actionIndex) == pointerId) endGesture(releaseVelocity())
            MotionEvent.ACTION_CANCEL -> endGesture()
        }
        return true
    }

    private fun releaseVelocity(): Float {
        val tracker = velocityTracker ?: return 0f
        tracker.computeCurrentVelocity(1000)
        return if (fromLeft) tracker.getXVelocity(pointerId) else -tracker.getYVelocity(pointerId)
    }

    // A finger that went down on the strip stays ours to take, whatever the view under it asks.
    override fun requestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
        if (!watching) super.requestDisallowInterceptTouchEvent(disallowIntercept)
    }

    override fun onDetachedFromWindow() {
        endGesture()
        super.onDetachedFromWindow()
    }
}
