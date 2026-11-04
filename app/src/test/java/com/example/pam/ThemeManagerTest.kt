package com.example.pam

import com.example.pam.ui.theme.ThemeManager
import com.example.pam.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeManagerTest {

    @Test
    fun testThemeModes() {
        assertEquals("Light", ThemeMode.LIGHT.title)
        assertEquals("Dark", ThemeMode.DARK.title)
        assertEquals("Device default", ThemeMode.SYSTEM.title)
    }

    @Test
    fun testSetThemeMode() {
        ThemeManager.setThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, ThemeManager.themeMode.value)

        ThemeManager.setThemeMode(ThemeMode.LIGHT)
        assertEquals(ThemeMode.LIGHT, ThemeManager.themeMode.value)

        ThemeManager.setThemeMode(ThemeMode.SYSTEM)
        assertEquals(ThemeMode.SYSTEM, ThemeManager.themeMode.value)
    }
}
