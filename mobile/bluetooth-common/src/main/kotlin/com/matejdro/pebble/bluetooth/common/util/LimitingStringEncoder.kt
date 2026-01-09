package com.matejdro.pebble.bluetooth.common.util

import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.StandardCharsets
import java.util.Arrays

class LimitingStringEncoder {
   private val utf8Encoder = StandardCharsets.UTF_8.newEncoder()

   fun encodeSizeLimited(text: String, maxSize: Int, ellipsize: Boolean = true): Result {
      if (ellipsize) {
         val encodedWithoutEllipsis = encodeSizeLimited(text, maxSize, ellipsize = false)
         if (!encodedWithoutEllipsis.wasTrimmed) {
            return encodedWithoutEllipsis
         }
      }

      val buffer = ByteBuffer.allocate(if (ellipsize) maxSize - ELLIPSIS.size else maxSize)

      val charBuffer = CharBuffer.wrap(text)
      val result = utf8Encoder.encode(charBuffer, buffer, true)

      val outputArray = if (ellipsize && result.isOverflow) {
         Arrays.copyOf(buffer.array(), buffer.position()) + ELLIPSIS
      } else {
         Arrays.copyOf(buffer.array(), buffer.position())
      }

      return Result(outputArray, result.isOverflow)
   }

   data class Result(val encodedString: ByteArray, val wasTrimmed: Boolean) {
      override fun equals(other: Any?): Boolean {
         if (this === other) return true
         if (other !is Result) return false

         if (wasTrimmed != other.wasTrimmed) return false
         if (!encodedString.contentEquals(other.encodedString)) return false

         return true
      }

      override fun hashCode(): Int {
         var result = wasTrimmed.hashCode()
         result = 31 * result + encodedString.contentHashCode()
         return result
      }
   }
}

private val ELLIPSIS = byteArrayOf(46, 46, 46)
