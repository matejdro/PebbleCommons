package com.matejdro.pebble.common.logging

import java.io.File

interface FileLoggingController {
   fun flush()

   fun getLogFolder(): File

   fun getDeviceInfo(): String
}
