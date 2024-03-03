package me.arasple.mc.trchat.module.internal.proxy

import me.arasple.mc.trchat.api.impl.BukkitProxyManager
import me.arasple.mc.trchat.api.nms.NMS
import me.arasple.mc.trchat.module.conf.Loader
import me.arasple.mc.trchat.module.display.channel.Channel
import me.arasple.mc.trchat.module.display.function.standard.EnderChestShow
import me.arasple.mc.trchat.module.display.function.standard.InventoryShow
import me.arasple.mc.trchat.module.display.function.standard.ItemShow
import me.arasple.mc.trchat.module.internal.TrChatBukkit
import me.arasple.mc.trchat.module.internal.proxy.redis.RedisManager
import me.arasple.mc.trchat.module.internal.proxy.redis.TrRedisMessage
import me.arasple.mc.trchat.util.*
import me.arasple.mc.trchat.util.proxy.common.MessageReader
import org.bukkit.Bukkit
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.plugin.messaging.PluginMessageListener
import org.bukkit.plugin.messaging.PluginMessageRecipient
import taboolib.common.platform.function.console
import taboolib.common.platform.function.getProxyPlayer
import taboolib.common.platform.function.submitAsync
import taboolib.common.util.subList
import taboolib.common5.util.decodeBase64
import taboolib.module.chat.Components
import taboolib.module.lang.asLangText
import taboolib.module.lang.sendLang
import taboolib.module.nms.MinecraftVersion
import taboolib.platform.util.bukkitPlugin
import taboolib.platform.util.deserializeToInventory
import taboolib.platform.util.onlinePlayers
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future

/**
 * @author ItsFlicker
 * @since 2022/6/18 16:17
 */
sealed interface BukkitProxyProcessor : PluginMessageListener {

    operator fun invoke(): BukkitProxyProcessor = this

    override fun onPluginMessageReceived(channel: String, player: Player, message: ByteArray) = Unit

    fun close() {
        Bukkit.getMessenger().unregisterIncomingPluginChannel(bukkitPlugin)
    }

    fun sendMessage(
        recipient: PluginMessageRecipient,
        executor: ExecutorService,
        data: Array<String>
    ): Future<*>

    fun execute(data: Array<String>) {
        when (data[0]) {
            "SendLang" -> {
                val to = data[1]
                val node = data[2]
                val args = subList(data.toList(), 3).toTypedArray()

                try {
                    getProxyPlayer(to)?.sendLang(node, *args)
                } catch (_: IllegalStateException) {
                }
            }
            "SendRaw" -> {
                val to = data[1]
                val raw = data[2]
                val message = Components.parseRaw(raw)

                getProxyPlayer(to)?.sendComponent(null, message)
            }
            "BroadcastRaw" -> {
                val uuid = data[1].toUUID()
                val raw = data[2]
                val perm = data[3]
                val ports = data[5].takeIf { it != "" }?.split(";")?.map { it.toInt() }
                val message = Components.parseRaw(raw)

                if (ports == null || BukkitProxyManager.port in ports) {
                    onlinePlayers
                        .filter { perm == "" || it.hasPermission(perm) }
                        .forEach { it.sendComponent(uuid, message) }
//                    console().sendComponent(uuid, message)
                }
            }
            "UpdateAllNames" -> {
                onlinePlayers.forEach {
                    NMS.instance.removeCustomChatCompletions(it, BukkitProxyManager.allPlayerNames.keys.toList())
                }
                BukkitProxyManager.updateNames()
                val names = data[1].takeIf { it != "" }?.split(",") ?: return
                BukkitProxyManager.allPlayerNames = data[2].split(",").mapIndexed { index, displayName ->
                    names[index] to displayName.takeIf { it != "null" }
                }.toMap()
                onlinePlayers.forEach {
                    NMS.instance.addCustomChatCompletions(it, BukkitProxyManager.allPlayerNames.keys.toList())
                }
            }
            "GlobalMute" -> {
                when (data[1]) {
                    "on" -> TrChatBukkit.isGlobalMuting = true
                    "off" -> TrChatBukkit.isGlobalMuting = false
                }
            }
            "SendProxyChannel" -> {
                val id = data[1]
                val channel = data[2]
                Loader.loadChannel(id, YamlConfiguration().also { it.loadFromString(channel) }).let {
                    Channel.channels[it.id] = it
                }
                BukkitProxyManager.sendMessage(onlinePlayers.firstOrNull(), arrayOf("LoadedProxyChannel", id))
            }
            "ItemShow" -> {
                if (data[1] == MinecraftVersion.minecraftVersion) {
                    val name = data[2]
                    val sha1 = data[3]
                    if (ItemShow.cacheHopper.getIfPresent(sha1) == null) {
                        val inventory = data[4].decodeBase64().deserializeToInventory(
                            createNoClickChest(3, console().asLangText("Function-Item-Show-Title", name))
                        )
                        ItemShow.cacheHopper.put(sha1, inventory)
                    }
                }
            }
            "InventoryShow" -> {
                if (data[1] == MinecraftVersion.minecraftVersion) {
                    val name = data[2]
                    val sha1 = data[3]
                    if (InventoryShow.cache.getIfPresent(sha1) == null) {
                        val inventory = data[4].decodeBase64().deserializeToInventory(
                            createNoClickChest(6, console().asLangText("Function-Inventory-Show-Title", name))
                        )
                        InventoryShow.cache.put(sha1, inventory)
                    }
                }
            }
            "EnderChestShow" -> {
                if (data[1] == MinecraftVersion.minecraftVersion) {
                    val name = data[2]
                    val sha1 = data[3]
                    if (EnderChestShow.cache.getIfPresent(sha1) == null) {
                        val inventory = data[4].decodeBase64().deserializeToInventory(
                            createNoClickChest(3, console().asLangText("Function-EnderChest-Show-Title", name))
                        )
                        EnderChestShow.cache.put(sha1, inventory)
                    }
                }
            }
        }
    }

    object BungeeSide : BukkitProxyProcessor {

        private const val TRCHAT_CHANNEL = "trchat:main"

        override operator fun invoke(): BukkitProxyProcessor {
            TRCHAT_CHANNEL.registerOutgoing()
            TRCHAT_CHANNEL.registerIncoming(this)
            return super.invoke()
        }

        override fun sendMessage(
            recipient: PluginMessageRecipient,
            executor: ExecutorService,
            data: Array<String>
        ): Future<*> {
            return executor.submit {
                try {
                    for (bytes in buildMessage(*data)) {
                        recipient.sendPluginMessage(bukkitPlugin, TRCHAT_CHANNEL, bytes)
                    }
                } catch (e: IOException) {
                    e.print("Failed to send proxy trchat message!")
                }
            }
        }

        override fun onPluginMessageReceived(channel: String, player: Player, message: ByteArray) {
            if (channel == TRCHAT_CHANNEL) {
                try {
                    val data = MessageReader.read(message)
                    if (data.isCompleted) {
                        execute(data.build())
                    }
                } catch (e: IOException) {
                    e.print("Failed to read proxy trchat message!")
                }
            }
        }
    }

    object VelocitySide : BukkitProxyProcessor {

        private const val TRCHAT_INCOMING = "trchat:server"
        private const val TRCHAT_OUTGOING = "trchat:proxy"

        override operator fun invoke(): BukkitProxyProcessor {
            TRCHAT_OUTGOING.registerOutgoing()
            TRCHAT_INCOMING.registerIncoming(this)
            return super.invoke()
        }

        override fun sendMessage(
            recipient: PluginMessageRecipient,
            executor: ExecutorService,
            data: Array<String>
        ): Future<*> {
            return executor.submit {
                try {
                    for (bytes in buildMessage(*data)) {
                        recipient.sendPluginMessage(bukkitPlugin, TRCHAT_OUTGOING, bytes)
                    }
                } catch (e: IOException) {
                    e.print("Failed to send proxy trchat message!")
                }
            }
        }

        override fun onPluginMessageReceived(channel: String, player: Player, message: ByteArray) {
            if (channel == TRCHAT_INCOMING) {
                try {
                    val data = MessageReader.read(message)
                    if (data.isCompleted) {
                        execute(data.build())
                    }
                } catch (e: IOException) {
                    e.print("Failed to read proxy trchat message!")
                }
            }
        }
    }

    object RedisSide : BukkitProxyProcessor {

        val allNames = mutableMapOf<Int, Map<String, String?>>()

        init {
            submitAsync(period = 200L) {
                BukkitProxyManager.updateNames()
            }
        }

        override fun execute(data: Array<String>) {
            when (data[0]) {
                "UpdateNames" -> {
                    onlinePlayers.forEach {
                        NMS.instance.removeCustomChatCompletions(it, BukkitProxyManager.allPlayerNames.keys.toList())
                    }
                    val port = data[2].toInt()
                    val names = data[1].takeIf { it != "" }?.split(",")?.map { it.split("-", limit = 2) } ?: return
                    allNames[port] = names.associate { it[0] to it[1].takeIf { dn -> dn != "null" } }
                    onlinePlayers.forEach {
                        NMS.instance.addCustomChatCompletions(it, BukkitProxyManager.allPlayerNames.keys.toList())
                    }
                }
                else -> super.execute(data)
            }
        }

        override fun sendMessage(
            recipient: PluginMessageRecipient,
            executor: ExecutorService,
            data: Array<String>
        ): Future<*> {
            return executor.submit {
                RedisManager.sendMessage(TrRedisMessage(data))
            }
        }
    }

    companion object {

        protected fun String.registerOutgoing() {
            if (!Bukkit.getMessenger().isOutgoingChannelRegistered(bukkitPlugin, this)) {
                Bukkit.getMessenger().registerOutgoingPluginChannel(bukkitPlugin, this)
            }
        }

        protected fun String.registerIncoming(listener: PluginMessageListener) {
            if (!Bukkit.getMessenger().isIncomingChannelRegistered(bukkitPlugin, this)) {
                Bukkit.getMessenger().registerIncomingPluginChannel(bukkitPlugin, this, listener)
            }
        }

    }
}