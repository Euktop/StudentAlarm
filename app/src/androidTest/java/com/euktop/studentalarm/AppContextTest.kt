package com.euktop.studentalarm

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppContextTest {
    @Test
    fun useAppContext(){
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.euktop.studentalarm", appContext.packageName)
    }
}