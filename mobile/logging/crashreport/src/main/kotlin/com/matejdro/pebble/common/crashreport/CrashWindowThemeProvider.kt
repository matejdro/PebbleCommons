package com.matejdro.pebble.common.crashreport

import androidx.compose.runtime.Composable

interface CrashWindowThemeProvider {
   @Composable
   fun ApplyTheme(content: @Composable () -> Unit)
}
