package com.matejdro.pebble.bluetooth.common.util

import java.nio.ByteBuffer
import java.nio.CharBuffer
import kotlin.experimental.and

/**
 * Pebble will not display any spaces or tabs at the beginning of the line, resulting in
 * indented texts not being indented.
 *
 * This works around the issue by replacing all those characters with non-breaking spaces that
 * are not affected by this.
 */
// It's pretty straightforward, just loop through characters
// and replace whitespace with a different whitespace. Not complex.
@Suppress("CognitiveComplexMethod")
fun String.fixPebbleIndentation(): String {
   val originalString = this

   return buildString(originalString.length) {
      var lineStart = true

      for (char in originalString) {
         if (char == '\n') {
            lineStart = true
            append(char)
         } else if (char == '\t') {
            if (lineStart) {
               // Indent tabs by two
               append(NON_BREAKING_SPACE)
               append(NON_BREAKING_SPACE)
            } else {
               append(char)
            }
         } else if (char.isWhitespace()) {
            if (lineStart) {
               append(NON_BREAKING_SPACE)
            } else {
               append(char)
            }
         } else {
            lineStart = false
            append(char)
         }
      }
   }
}

/**
 * If this byte buffer ends with an invalid-encoded UTF-8 sequence (such as UTF-8 encoding being cut in the middle),
 * this method will move position of the buffer back to just before such a sequence and move input character buffer to before
 * the offending input character.
 */
@Suppress("NestedBlockDepth") // Self contained algorithm
fun ByteBuffer.trimLastInvalidUtf8Character(input: CharBuffer) {
   if (input.position() > 0) {
      val lastWrittenBytePosition = this.position() - 1
      val lastWrittenChar = input.get(input.position() - 1)

      if (lastWrittenChar > 0x007F.toChar()) {
         // Last written char is UTF-8 multi-char codepoint
         for (i in lastWrittenBytePosition downTo 0) {
            val utf8SeparatorSize = this.get(i).getUtf8CharLengthFromSeparator()
            if (utf8SeparatorSize != -1) {
               val bytesAfter = lastWrittenBytePosition - i
               if (bytesAfter < utf8SeparatorSize) {
                  this.position(i)
                  input.position(input.position() - 1)
               }
               break
            }
         }
      }
   }
}

/**
 * Returns -1 if a character is not a UTF-8 separator (first byte). Otherwise, it returns the number of all bytes that follow
 * this separator for a valid UTF-8 encoding
 */
@Suppress("MagicNumber") // Number from UTF8 spec
private fun Byte.getUtf8CharLengthFromSeparator(): Int {
   return when {
      (this and 0b10000000.toByte()) == 0.toByte() -> -1
      (this and 0b11100000.toByte()) == 0b11000000.toByte() -> 1
      (this and 0b11110000.toByte()) == 0b11100000.toByte() -> 2
      (this and 0b11111000.toByte()) == 0b11110000.toByte() -> 3
      else -> -1
   }
}

private const val NON_BREAKING_SPACE = 160.toChar()
