package com.ivianuu.conductor.internal

import android.text.TextUtils

internal object ClassUtils {

    fun <T> classForName(className: String, allowEmptyName: Boolean): Class<out T>? {
        if (allowEmptyName && TextUtils.isEmpty(className)) {
            return null
        }

        try {
            return Class.forName(className) as Class<out T>
        } catch (e: Exception) {
            throw RuntimeException("An exception occurred while finding class for name " + className + ". " + e.message)
        }

    }

    fun <T> newInstance(className: String): T? {
        try {
            val cls = classForName<T>(className, true)
            return cls?.newInstance()
        } catch (e: Exception) {
            throw RuntimeException("An exception occurred while creating a new instance of " + className + ". " + e.message)
        }

    }

}
