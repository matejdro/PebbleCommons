package com.matejdro.pebble.bluetooth.common

import io.rebble.pebblekit2.common.model.PebbleDictionary
import io.rebble.pebblekit2.common.model.ReceiveResult
import io.rebble.pebblekit2.common.model.WatchIdentifier
import kotlinx.coroutines.CoroutineScope
import com.matejdro.catapult.bluetooth.WatchNotificationMessage

interface WatchAppConnection {
   suspend fun onPacketReceived(data: PebbleDictionary): ReceiveResult
   suspend fun sendInteractivePackets(packets: List<PebbleDictionary>)
   suspend fun sendNotification(packet: PebbleDictionary)
   suspend fun sendNotification(notification: WatchNotificationMessage.Show)

   fun interface Factory {
      fun create(
         watch: WatchIdentifier,
         scope: CoroutineScope,
      ): WatchAppConnection
   }
}
