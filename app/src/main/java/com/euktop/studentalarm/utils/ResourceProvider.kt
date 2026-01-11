package com.euktop.studentalarm.utils

import android.content.Context
import androidx.annotation.StringRes

class ResourceProvider(private val context: Context) {

    fun getString(@StringRes resId: Int): String = context.getString(resId)

    fun getString(@StringRes resId: Int, vararg args: Any): String =
        context.getString(resId, *args)

    fun getColor(@StringRes resId: Int): Int = context.getColor(resId)
}