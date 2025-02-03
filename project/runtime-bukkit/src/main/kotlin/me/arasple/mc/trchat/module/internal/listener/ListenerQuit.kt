package me.arasple.mc.trchat.module.internal.listener

import me.arasple.mc.trchat.module.display.ChatSession
import me.arasple.mc.trchat.module.display.channel.Channel
import me.arasple.mc.trchat.module.internal.data.PlayerData
import org.bukkit.event.player.PlayerQuitEvent
import taboolib.common.platform.Platform
import taboolib.common.platform.PlatformSide
import taboolib.common.platform.event.EventPriority
import taboolib.common.platform.event.SubscribeEvent
import taboolib.expansion.playerDataContainer
import taboolib.expansion.releaseDataContainer

/**
 * @author ItsFlicker
 * @since 2021/12/11 23:19
 */
@PlatformSide(Platform.BUKKIT)
object ListenerQuit {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onQuit(e: PlayerQuitEvent) {
        val player = e.player

        if (!playerDataContainer.containsKey(player.uniqueId)) return

        Channel.channels.values.forEach { it.listeners -= player.name }
        ChatSession.removeSession(player)
        PlayerData.removeData(player)
        player.releaseDataContainer()
    }

}