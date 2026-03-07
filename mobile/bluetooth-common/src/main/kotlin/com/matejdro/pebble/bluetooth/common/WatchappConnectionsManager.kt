package com.matejdro.pebble.bluetooth.common

import io.rebble.pebblekit2.common.model.PebbleDictionary
import io.rebble.pebblekit2.common.model.ReceiveResult
import io.rebble.pebblekit2.common.model.WatchIdentifier
import java.util.UUID

interface WatchappConnectionsManager {
   suspend fun onMessageReceived(
      watchappUUID: UUID,
      data: PebbleDictionary,
      watch: WatchIdentifier,
   ): ReceiveResult

   fun onAppOpened(
      watchappUUID: UUID,
      watch: WatchIdentifier,
   )

   fun onAppClosed(
      watchappUUID: UUID,
      watch: WatchIdentifier,
   )
}
