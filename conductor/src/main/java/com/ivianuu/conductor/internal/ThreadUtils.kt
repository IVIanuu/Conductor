package com.ivianuu.conductor.internal

import android.os.Looper
import android.util.AndroidRuntimeException

object ThreadUtils {

    @JvmStatic
    fun ensureMainThread() {
        if (Looper.getMainLooper().thread != Thread.currentThread()) {
            throw CalledFromWrongThreadException("Methods that affect the view hierarchy can can only be called from the main thread.")
        }
    }

    private class CalledFromWrongThreadException internal constructor(msg: String) :
        AndroidRuntimeException(msg)

}
