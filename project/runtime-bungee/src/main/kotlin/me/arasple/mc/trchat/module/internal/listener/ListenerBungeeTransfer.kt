package me.arasple.mc.trchat.module.internal.listener

import me.arasple.mc.trchat.api.impl.BungeeComponentManager
import me.arasple.mc.trchat.api.impl.BungeeProxyManager
import me.arasple.mc.trchat.module.internal.TrChatBungee
import me.arasple.mc.trchat.util.print
import me.arasple.mc.trchat.util.proxy.common.MessageReader
import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.connection.Connection
import net.md_5.bungee.api.event.PluginMessageEvent
import taboolib.common.platform.Platform
import taboolib.common.platform.PlatformSide
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.console
import taboolib.common.platform.function.server
import taboolib.module.chat.Components
import java.io.IOException

/**
 * ListenerBungeeTransfer
 * me.arasple.mc.trchat.util.proxy.bungee
 *
 * @author ItsFlicker
 * @since 2021/8/9 15:01
 */
@PlatformSide(Platform.BUNGEE)
object ListenerBungeeTransfer {

    @SubscribeEvent(level = 0)
    fun onTransfer(e: PluginMessageEvent) {
        if (e.isCancelled) {
            return
        }
        if (e.tag == TrChatBungee.TRCHAT_CHANNEL) {
            try {
                val message = MessageReader.read(e.data)
                if (message.isCompleted) {
                    val data = message.build()
                    execute(data, e.sender)
                }
            } catch (ex: IOException) {
                ex.print("Error occurred while reading plugin message.")
            }
        }
    }

    @Suppress("Deprecation")
    private fun execute(data: Array<String>, connection: Connection) {
        when (data[0]) {
            "ForwardMessage" -> {
                BungeeProxyManager.sendMessageToAll(*data)
            }
            "BroadcastRaw" -> {
                val uuid = data[1]
                val raw = data[2]
                val perm = data[3]
                val doubleTransfer = data[4].toBoolean()
                val ports = data[5].takeIf { it != "" }?.split(";")?.map { it.toInt() }
                val message = try {
                    Components.parseRaw(raw).also { it.sendTo(console()) }
                } catch (e: Throwable) {
                    Components.text("Unable to parse raw message!")
                }

                if (doubleTransfer) {
                    BungeeProxyManager.sendMessageToAll("BroadcastRaw", uuid, raw, perm, data[4], data[5]) {
                        ports == null || it.address.port in ports
                    }
                } else {
                    server<ProxyServer>().servers.forEach { (_, v) ->
                        if (ports == null || v.address.port in ports) {
                            v.players.filter { perm == "" || it.hasPermission(perm) }.forEach {
                                BungeeComponentManager.sendComponent(it, message, uuid)
                            }
                        }
                    }
                }
            }
            "UpdateNames" -> {
                val names = data[1].split(",").map { it.split("-", limit = 2) }
                BungeeProxyManager.allNames[connection.address.port] = names.associate { it[0] to it[1].takeIf { dn -> dn != "null" } }
            }
        }
    }
}