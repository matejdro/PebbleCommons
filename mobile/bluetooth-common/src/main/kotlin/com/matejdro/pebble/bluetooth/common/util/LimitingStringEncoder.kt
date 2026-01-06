package com.matejdro.pebble.bluetooth.common.util

import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.StandardCharsets
import java.util.Arrays

class LimitingStringEncoder() {
   private val utf8Encoder = StandardCharsets.UTF_8.newEncoder()

   fun encodeSizeLimited(text: String, maxSize: Int, ellipsize: Boolean = true): ByteArray {
      val buffer = ByteBuffer.allocate(if (ellipsize) maxSize - ELLIPSIS.size else maxSize)

      val charBuffer = CharBuffer.wrap(text)
      val result = utf8Encoder.encode(charBuffer, buffer, true)

      return if (ellipsize && result.isOverflow) {
         Arrays.copyOf(buffer.array(), buffer.position()) + ELLIPSIS
      } else {
         Arrays.copyOf(buffer.array(), buffer.position())
      }
   }
}

private val ELLIPSIS = byteArrayOf(46, 46, 46)
