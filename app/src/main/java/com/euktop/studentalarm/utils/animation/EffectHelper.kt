package com.euktop.studentalarm.utils.animation

import android.view.View

object EffectHelper {
    fun setupRippleEffect(view: View) {
        val attrs = intArrayOf(android.R.attr.selectableItemBackground)
        val typedArray = view.context.obtainStyledAttributes(attrs)
        val backgroundResource = typedArray.getResourceId(0, 0)
        typedArray.recycle()

        view.apply {
            isClickable = true
            foreground = ContextCompat.getDrawable(context, backgroundResource)
        }
    }
}
