package com.euktop.studentalarm.utils.animation

import android.animation.ObjectAnimator
import android.view.View
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.view.isGone
import com.euktop.studentalarm.R

object AnimatorHelper {

    enum class Direction {
        UP, DOWN, LEFT, RIGHT
    }

    private data class AnimationValues(
        val fromX: Float,
        val toX: Float,
        val fromY: Float,
        val toY: Float
    )

/*    fun slideView(
        view: View,
        fromY: Float,
        toY: Float,
        duration: Long,
        onStart: (() -> Unit)? = null,
        onEnd: (() -> Unit)? = null
    ) {
        view.visibility = View.VISIBLE
        val animator = ObjectAnimator.ofFloat(view, "translationY", fromY, toY)
        animator.duration = duration
        animator.doOnStart { onStart?.invoke() }
        animator.doOnEnd { onEnd?.invoke() }
        animator.start()
    }*/

    fun slideViewFromDirection(
        view: View,
        direction: Direction,
        duration: Long,
        onStart: (() -> Unit)? = null,
        onEnd: (() -> Unit)? = null
    ) {
        if (view.isGone) {
            view.visibility = View.INVISIBLE
            view.post {
                slideViewFromDirectionInternal(view, direction, duration, onStart, onEnd)
            }
        } else {
            slideViewFromDirectionInternal(view, direction, duration, onStart, onEnd)
        }
    }

    private fun slideViewFromDirectionInternal(
        view: View,
        direction: Direction,
        duration: Long,
        onStart: (() -> Unit)?,
        onEnd: (() -> Unit)?
    ) {
        val values = when (direction) {
            Direction.UP -> AnimationValues(0f, 0f, view.measuredHeight.toFloat(), 0f)
            Direction.DOWN -> AnimationValues(0f, 0f, -view.measuredHeight.toFloat(), 0f)
            Direction.LEFT -> AnimationValues(view.measuredWidth.toFloat(), 0f, 0f, 0f)
            Direction.RIGHT -> AnimationValues(-view.measuredWidth.toFloat(), 0f, 0f, 0f)
        }

        slideViewWithBothAxes(
            view,
            values.fromX,
            values.toX,
            values.fromY,
            values.toY,
            duration,
            onStart,
            onEnd
        )
    }

    fun slideViewToDirection(
        view: View,
        direction: Direction,
        duration: Long,
        onStart: (() -> Unit)? = null,
        onEnd: (() -> Unit)? = null
    ) {
        val values = when (direction) {
            Direction.UP -> AnimationValues(0f, 0f, 0f, -view.measuredHeight.toFloat())
            Direction.DOWN -> AnimationValues(0f, 0f, 0f, view.measuredHeight.toFloat())
            Direction.LEFT -> AnimationValues(0f, -view.measuredWidth.toFloat(), 0f, 0f)
            Direction.RIGHT -> AnimationValues(0f, view.measuredWidth.toFloat(), 0f, 0f)
        }

        slideViewWithBothAxes(
            view,
            values.fromX,
            values.toX,
            values.fromY,
            values.toY,
            duration,
            onStart,
            onEnd
        )
    }

    private fun slideViewWithBothAxes(
        view: View,
        fromX: Float,
        toX: Float,
        fromY: Float,
        toY: Float,
        duration: Long,
        onStart: (() -> Unit)?,
        onEnd: (() -> Unit)?
    ) {
        view.visibility = View.VISIBLE
        view.translationX = fromX
        view.translationY = fromY

        val animatorX = ObjectAnimator.ofFloat(view, "translationX", fromX, toX)
        val animatorY = ObjectAnimator.ofFloat(view, "translationY", fromY, toY)

        animatorX.duration = duration
        animatorY.duration = duration

        animatorX.doOnStart { onStart?.invoke() }
        animatorX.doOnEnd { onEnd?.invoke() }

        animatorX.start()
        animatorY.start()
    }

/*    fun fadeView(
        view: View,
        fromAlpha: Float,
        toAlpha: Float,
        duration: Long,
        onStart: (() -> Unit)? = null,
        onEnd: (() -> Unit)? = null
    ) {
        view.visibility = View.VISIBLE
        val animator = ObjectAnimator.ofFloat(view, "alpha", fromAlpha, toAlpha)
        animator.duration = duration
        animator.doOnStart { onStart?.invoke() }
        animator.doOnEnd { onEnd?.invoke() }
        animator.start()
    }

    fun scaleView(
        view: View,
        fromX: Float,
        toX: Float,
        fromY: Float,
        toY: Float,
        duration: Long,
        onStart: (() -> Unit)? = null,
        onEnd: (() -> Unit)? = null
    ) {
        view.visibility = View.VISIBLE
        val animatorX = ObjectAnimator.ofFloat(view, "scaleX", fromX, toX)
        val animatorY = ObjectAnimator.ofFloat(view, "scaleY", fromY, toY)

        animatorX.duration = duration
        animatorY.duration = duration

        animatorX.doOnStart { onStart?.invoke() }
        animatorX.doOnEnd { onEnd?.invoke() }

        animatorX.start()
        animatorY.start()
    }*/

    fun bounceView(
        view: View,
        scale: Float = 1.1f,
        duration: Long = R.integer.anim_duration.toLong()
    ) {
        val originalScaleX = view.scaleX
        val originalScaleY = view.scaleY

        view.animate()
            .scaleX(scale)
            .scaleY(scale)
            .setDuration(duration)
            .withEndAction {
                view.animate()
                    .scaleX(originalScaleX)
                    .scaleY(originalScaleY)
                    .setDuration(duration)
                    .start()
            }
            .start()
    }
}