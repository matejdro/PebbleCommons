package com.matejdro.pebble.bluetooth.common.util

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class StringUtilsTest {
   @Test
   fun `fixIndentation - process spaces`() {
      "a b c d\n  e f g h".fixPebbleIndentation() shouldBe "a b c d\n\u00a0\u00a0e f g h"
   }

   @Test
   fun `fixIndentation - process tabs`() {
      "a b c d\n\t\te f g h".fixPebbleIndentation() shouldBe "a b c d\n\u00a0\u00a0\u00a0\u00a0e f g h"
   }
}
