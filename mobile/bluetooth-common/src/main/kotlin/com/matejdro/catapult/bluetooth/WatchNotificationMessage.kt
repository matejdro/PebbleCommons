package com.matejdro.catapult.bluetooth

import io.rebble.pebblekit2.common.model.PebbleDictionary
import io.rebble.pebblekit2.common.model.PebbleDictionaryItem

sealed interface WatchNotificationMessage {
   data class Show(
      val title: String,
      val body: String,
      val vibration: Vibration,
      val durationMs: Long,
   ) : WatchNotificationMessage {
      init {
         require(title.utf8Length() <= MAX_TITLE_BYTES) { "Notification title is too long" }
         require(body.utf8Length() <= MAX_BODY_BYTES) { "Notification body is too long" }
         require(durationMs in 0..MAX_DURATION_MS) { "Notification duration is out of range" }
      }

      fun toPacket(maxPayloadBytes: Int): PebbleDictionary {
         require(maxPayloadBytes > 0) { "Payload limit must be positive" }
         val packet = mapOf(
            KEY_TYPE to PebbleDictionaryItem.UInt32(PACKET_SHOW_NOTIFICATION),
            KEY_TITLE to PebbleDictionaryItem.Text(title),
            KEY_BODY to PebbleDictionaryItem.Text(body),
            KEY_VIBRATION to PebbleDictionaryItem.UInt8(vibration.wireValue),
            KEY_DURATION_MS to PebbleDictionaryItem.UInt32(durationMs.toUInt()),
         )
         val encoded = 1 + packet.values.sumOf { DICTIONARY_ITEM_OVERHEAD_BYTES + it.encodedPayloadSize() }
         require(encoded < maxPayloadBytes) { "Notification packet exceeds watch buffer ($encoded >= $maxPayloadBytes)" }
         return packet
      }
   }

   enum class Vibration(val wireValue: UByte) {
      NONE(0u),
      SHORT(1u),
      DOUBLE(2u),
   }

   companion object {
      const val PACKET_SHOW_NOTIFICATION = 11u

      private const val KEY_TYPE = 0u
      private const val KEY_TITLE = 2u
      private const val KEY_VIBRATION = 6u
      private const val KEY_BODY = 7u
      private const val KEY_DURATION_MS = 8u

      const val MAX_TITLE_BYTES = 64
      const val MAX_BODY_BYTES = 128
      const val MAX_DURATION_MS = 300_000L

      /** Wire overhead (key + type + length header) added per dictionary item when encoding a packet. */
      private const val DICTIONARY_ITEM_OVERHEAD_BYTES = 7
   }
}

private fun String.utf8Length() = toByteArray(Charsets.UTF_8).size

private const val UINT32_BYTE_SIZE = 4

private fun PebbleDictionaryItem.encodedPayloadSize(): Int = when (this) {
   is PebbleDictionaryItem.Text -> value.utf8Length() + 1
   is PebbleDictionaryItem.Bytes -> value.size
   is PebbleDictionaryItem.UInt8, is PebbleDictionaryItem.Int8 -> 1
   is PebbleDictionaryItem.UInt16, is PebbleDictionaryItem.Int16 -> 2
   is PebbleDictionaryItem.UInt32, is PebbleDictionaryItem.Int32 -> UINT32_BYTE_SIZE
}
