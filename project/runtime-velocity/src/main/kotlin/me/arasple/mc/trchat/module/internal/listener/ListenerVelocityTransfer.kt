package me.arasple.mc.trchat.module.internal.listener

import com.velocitypowered.api.event.connection.PluginMessageEvent
import com.velocitypowered.api.proxy.ServerConnection
import me.arasple.mc.trchat.api.impl.VelocityProxyManager
import me.arasple.mc.trchat.module.internal.TrChatVelocity.plugin
import me.arasple.mc.trchat.util.print
import me.arasple.mc.trchat.util.proxy.common.MessageReader
import me.arasple.mc.trchat.util.toUUID
import net.kyori.adventure.audience.MessageType
import net.kyori.adventure.identity.Identity
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import taboolib.common.platform.Platform
import taboolib.common.platform.PlatformSide
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.warning
import java.io.IOException

/**
 * ListenerVelocityTransfer
 * me.arasple.mc.trchat.util.proxy.velocity
 *
 * @author ItsFlicker
 * @since 2021/8/21 13:29
 */
@PlatformSide(Platform.VELOCITY)
object ListenerVelocityTransfer {

    @SubscribeEvent
    fun onTransfer(e: PluginMessageEvent) {
        if (e.identifier != VelocityProxyManager.incoming) return
        val source = e.source
        if (source !is ServerConnection) {
            warning("Received trchat:proxy message, but source is not ServerConnection")
            return
        }
        try {
            val message = MessageReader.read(e.data)
            if (message.isCompleted) {
                val data = message.build()
                execute(data, source)
            }
        } catch (ex: IOException) {
            ex.print("Error occurred while reading plugin message.")
        }
    }

    private fun execute(data: Array<String>, connection: ServerConnection) {
        when (data[0]) {
            "ForwardMessage" -> {
                VelocityProxyManager.sendMessageToAll(*data)
            }
            "BroadcastRaw" -> {
                val uuid = data[1]
                val raw = data[2]
                val perm = data[3]
                val doubleTransfer = data[4].toBoolean()
                val ports = data[5].takeIf { it != "" }?.split(";")?.map { it.toInt() }
                val message = GsonComponentSerializer.gson().deserialize(raw)
                plugin.server.consoleCommandSource.sendMessage(message)

                if (doubleTransfer) {
                    VelocityProxyManager.sendMessageToAll("BroadcastRaw", uuid, raw, perm, data[4], data[5]) {
                        ports == null || it.serverInfo.address.port in ports
                    }
                } else {
                    plugin.server.allServers.forEach { server ->
                        if (ports == null || server.serverInfo.address.port in ports) {
                            server.playersConnected.filter { perm == "" || it.hasPermission(perm) }.forEach {
                                it.sendMessage(Identity.identity(uuid.toUUID()), message, MessageType.CHAT)
                            }
                        }
                    }
                }
            }
            "UpdateNames" -> {
                val names = data[1].split(",").map { it.split("-", limit = 2) }
                VelocityProxyManager.allNames[connection.serverInfo.address.port] = names.associate { it[0] to it[1].takeIf { dn -> dn != "null" } }
            }
        }
    }
}