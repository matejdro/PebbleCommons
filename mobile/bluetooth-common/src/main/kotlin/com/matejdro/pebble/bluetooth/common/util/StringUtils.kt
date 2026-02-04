package com.matejdro.pebble.bluetooth.common.util

/**
 * Pebble will not display any spaces or tabs at the beginning of the line, resulting in
 * indented texts not being indented.
 *
 * This works around the issue by replacing all those characters with non-breaking spaces that
 * are not affected by this.
 */
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

private const val NON_BREAKING_SPACE = 160.toChar()
