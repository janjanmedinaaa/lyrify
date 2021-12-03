package com.medina.juanantonio.lyrify.common.views

import android.content.Context
import android.content.res.Resources
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.MotionEvent.INVALID_POINTER_ID
import android.view.View

class PlayerTouchView(
    context: Context,
    attributeSet: AttributeSet
) : View(context, attributeSet) {

    private val skipPreviousSection: Pair<Int, Int>
    private val playPauseSection: Pair<Int, Int>
    private val skipNextSection: Pair<Int, Int>

    private var onTouchEventListener: (Int) -> Unit = {}
    private var onPlayerClicked: (PlayerAction) -> Unit = {}

    // (mLastTouchX, mLastTouchY, mPosX, mPosY)
    private var onPlayerSwiped: (Float, Float, Float, Float) -> Unit = { _, _, _, _ -> }

    init {
        val (widthPixels, _) = Resources.getSystem().displayMetrics.let {
            it.widthPixels to it.heightPixels
        }

        val sectionSize = widthPixels / 3
        skipPreviousSection = Pair(0, sectionSize)
        playPauseSection = Pair(sectionSize + 1, sectionSize * 2)
        skipNextSection = Pair((sectionSize * 2) + 1, widthPixels)
    }

    // The ‘active pointer’ is the one currently moving our object.
    private var mActivePointerId = INVALID_POINTER_ID

    // Last position stored
    private var mLastTouchX: Float = 0f
    private var mLastTouchY: Float = 0f

    // Position diff from start (ACTION_DOWN) to finish (ACTION_UP)
    private var mPosX: Float = 0f
    private var mPosY: Float = 0f

    fun setOnTouchEvent(onTouchEventListener: (Int) -> Unit) {
        this.onTouchEventListener = onTouchEventListener
    }

    fun setOnPlayerClicked(onPlayerClicked: (PlayerAction) -> Unit) {
        this.onPlayerClicked = onPlayerClicked
    }

    fun setOnPlayerSwiped(onPlayerSwiped: (Float, Float, Float, Float) -> Unit) {
        this.onPlayerSwiped = onPlayerSwiped
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        onTouchEventListener(event.actionMasked)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                event.actionIndex.also { pointerIndex ->
                    // Remember where we started (for dragging)
                    mLastTouchX = event.getX(pointerIndex)
                    mLastTouchY = event.getY(pointerIndex)
                }

                // Save the ID of this pointer (for dragging)
                mActivePointerId = event.getPointerId(0)
            }

            MotionEvent.ACTION_MOVE -> {
                // Find the index of the active pointer and fetch its position
                val (x: Float, y: Float) =
                    event.findPointerIndex(mActivePointerId).let { pointerIndex ->
                        // Calculate the distance moved
                        event.getX(pointerIndex) to event.getY(pointerIndex)
                    }

                mPosX += x - mLastTouchX
                mPosY += mLastTouchY - y

                invalidate()

                // Remember this touch position for the next move event
                mLastTouchX = x
                mLastTouchY = y

                onPlayerSwiped(mLastTouchX, mLastTouchY, mPosX, mPosY)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                mActivePointerId = INVALID_POINTER_ID

                // Check if user performed a click
                if (mPosX == 0f && mPosY == 0f) {
                    when {
                        (skipPreviousSection.first..skipPreviousSection.second)
                            .contains(mLastTouchX.toInt()) -> {
                            onPlayerClicked(PlayerAction.SKIP_PREVIOUS)
                        }
                        (playPauseSection.first..playPauseSection.second)
                            .contains(mLastTouchX.toInt()) -> {
                            onPlayerClicked(PlayerAction.PLAY_PAUSE)
                        }
                        (skipNextSection.first..skipNextSection.second)
                            .contains(mLastTouchX.toInt()) -> {
                            onPlayerClicked(PlayerAction.SKIP_NEXT)
                        }
                    }
                }

                // Reset position diff when finger is removed
                mPosX = 0f
                mPosY = 0f
            }
            MotionEvent.ACTION_POINTER_UP -> {
                event.actionIndex.also { pointerIndex ->
                    event.getPointerId(pointerIndex)
                        .takeIf { it == mActivePointerId }
                        ?.run {
                            // This was our active pointer going up. Choose a new
                            // active pointer and adjust accordingly.
                            val newPointerIndex = if (pointerIndex == 0) 1 else 0
                            mLastTouchX = event.getX(newPointerIndex)
                            mLastTouchY = event.getY(newPointerIndex)
                            mActivePointerId = event.getPointerId(newPointerIndex)
                        }
                }
            }
        }

        return true
    }

    enum class PlayerAction {
        SKIP_PREVIOUS,
        PLAY_PAUSE,
        SKIP_NEXT
    }
}