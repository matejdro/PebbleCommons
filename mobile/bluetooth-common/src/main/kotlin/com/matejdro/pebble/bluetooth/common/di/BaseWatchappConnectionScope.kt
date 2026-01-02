package com.matejdro.pebble.bluetooth.common.di

import com.matejdro.pebble.bluetooth.common.WatchAppConnection
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.GraphExtension
import dev.zacsweers.metro.Provides
import io.rebble.pebblekit2.common.model.WatchIdentifier
import kotlinx.coroutines.CoroutineScope

@GraphExtension(WatchappConnectionScope::class)
interface WatchappConnectionGraph {
   fun createWatchappConnection(): WatchAppConnection

   @ContributesTo(AppScope::class)
   @GraphExtension.Factory
   interface Factory {
      fun create(
         @Provides
         scope: CoroutineScope,
         @Provides
         watch: WatchIdentifier,
      ): WatchappConnectionGraph
   }
}

abstract class WatchappConnectionScope
