package me.arasple.mc.trchat.module.internal.listener

import me.arasple.mc.trchat.module.display.ChatSession
import taboolib.common.platform.Platform
import taboolib.common.platform.PlatformSide
import taboolib.common.platform.event.SubscribeEvent
import taboolib.module.configuration.ConfigNode
import taboolib.module.nms.MinecraftVersion.versionId
import taboolib.module.nms.PacketReceiveEvent
import taboolib.module.nms.PacketSendEvent

/**
 * @author Arasple
 * @date 2019/11/30 10:16
 */
@PlatformSide(Platform.BUKKIT)
object ListenerPackets {

    @ConfigNode("Options.Cheat-Client-Secure-Chat", "settings.yml")
    var cheatClientSecureChat = true
        private set

    /**
     * 去除进入时右上角提示/禁止聊天举报
     * TODO Support 1.21
     */
    @SubscribeEvent
    fun secure(e: PacketSendEvent) {
        if (!cheatClientSecureChat) return
        if (versionId < 11902) return
        when (e.packet.name) {
            "ClientboundServerDataPacket" -> {
                if (versionId < 12005) e.packet.write("enforcesSecureChat", true)
            }
        }
    }

    @SubscribeEvent
    fun secure(e: PacketReceiveEvent) {
        if (versionId >= 12005) return
        if (e.packet.name == "ServerboundChatSessionUpdatePacket") {
            e.isCancelled = true
        }
    }

    /**
     * 记录玩家收到的消息
     */
    @SubscribeEvent
    fun record(e: PacketSendEvent) {
        if (versionId >= 12005) return
        val session = ChatSession.sessions[e.player.uniqueId] ?: return
        when (e.packet.name) {
            "ClientboundSystemChatPacket" -> {
                session.addMessage(e.packet)
            }
            "PacketPlayOutChat" -> {
                val type = if (versionId >= 11700) {
                    e.packet.read<Byte>("type/index")!!
                } else if (versionId >= 11200) {
                    e.packet.read<Byte>("b/d")!!
                } else {
                    e.packet.read<Byte>("b")
                }
                if (type != 0.toByte()) {
                    return
                }
                session.addMessage(e.packet)
            }
        }
    }
}