package com.matejdro.pebble.bluetooth.common.util

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class LimitingStringEncoderTest {
   private val encoder = LimitingStringEncoder()

   @Test
   fun `Encode short string`() {
      val text = "Hello"

      val encoded = encoder.encodeSizeLimited(text, 10, ellipsize = false)

      encoded shouldBe LimitingStringEncoder.Result(byteArrayOf(72, 101, 108, 108, 111), false)
   }

   @Test
   fun `Encode long string without ellipsis`() {
      val text = "a".repeat(20)

      val encoded = encoder.encodeSizeLimited(text, 10, ellipsize = false)

      encoded shouldBe LimitingStringEncoder.Result(ByteArray(10) { 'a'.code.toByte() }, true)
   }

   @Test
   fun `Encode long string with ellipsis`() {
      val text = "a".repeat(20)

      val encoded = encoder.encodeSizeLimited(text, 10, ellipsize = true)

      encoded shouldBe LimitingStringEncoder.Result(ByteArray(7) { 'a'.code.toByte() } + byteArrayOf(46, 46, 46), true)
   }

   @Test
   fun `Do not add ellipsis when string is just large enough`() {
      val text = "a".repeat(10)

      val encoded = encoder.encodeSizeLimited(text, 10, ellipsize = true)

      encoded shouldBe LimitingStringEncoder.Result(ByteArray(10) { 'a'.code.toByte() }, false)
   }
}
