package me.arasple.mc.trchat.api.impl

import com.google.common.util.concurrent.ThreadFactoryBuilder
import me.arasple.mc.trchat.api.ClientMessageManager
import me.arasple.mc.trchat.api.ProxyMode
import me.arasple.mc.trchat.module.conf.file.Settings
import me.arasple.mc.trchat.module.internal.data.PlayerData
import me.arasple.mc.trchat.module.internal.proxy.BukkitProxyProcessor
import me.arasple.mc.trchat.module.internal.proxy.redis.RedisManager
import me.arasple.mc.trchat.util.parseString
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.plugin.messaging.PluginMessageRecipient
import org.spigotmc.SpigotConfig
import taboolib.common.platform.Platform
import taboolib.common.platform.PlatformFactory
import taboolib.common.platform.PlatformSide
import taboolib.common.platform.function.console
import taboolib.common.platform.function.getProxyPlayer
import taboolib.common.util.unsafeLazy
import taboolib.common5.cint
import taboolib.module.chat.ComponentText
import taboolib.module.lang.sendLang
import taboolib.platform.util.onlinePlayers
import java.io.FileInputStream
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

/**
 * @author ItsFlicker
 * @since 2022/6/18 15:43
 */
@Suppress("Deprecation")
@PlatformSide(Platform.BUKKIT)
object BukkitProxyManager : ClientMessageManager {

    override var port = 25565

    var allPlayerNames = mapOf<String, String?>()
        get() = if (mode == ProxyMode.NONE) {
            onlinePlayers.associate { it.name to ChatColor.stripColor(it.displayName) }
        } else if (mode == ProxyMode.REDIS) {
            val result = mutableMapOf<String, String?>()
            (processor as BukkitProxyProcessor.RedisSide).allNames.values.forEach { result += it }
            result
        } else {
            field
        }

    init {
        PlatformFactory.registerAPI<ClientMessageManager>(this)
        FileInputStream("server.properties").use {
            val props = Properties()
            props.load(it)
            port = props.getProperty("server-port")?.cint ?: 25565
        }
    }

    override val executor: ExecutorService by unsafeLazy {
        val factory = ThreadFactoryBuilder().setNameFormat("TrChat PluginMessage Processing Thread #%d").build()
        Executors.newFixedThreadPool(8, factory)
    }

    override val mode: ProxyMode by unsafeLazy {
        val force = kotlin.runCatching {
            ProxyMode.valueOf(Settings.conf.getString("Options.Proxy")?.uppercase() ?: "AUTO")
        }
        if (force.isSuccess) {
            return@unsafeLazy force.getOrThrow()
        }
        if (RedisManager.enabled) {
            console().sendLang("Plugin-Proxy-Supported", "Redis")
            ProxyMode.REDIS
        } else if (SpigotConfig.bungee) {
            console().sendLang("Plugin-Proxy-Supported", "Bungee")
            ProxyMode.BUNGEE
        } else if (kotlin.runCatching {
                Bukkit.spigot().paperConfig.getBoolean("proxies.velocity.enabled", false) ||
                        Bukkit.spigot().paperConfig.getBoolean("settings.velocity-support.enabled", false)
            }.getOrDefault(false)) {
            console().sendLang("Plugin-Proxy-Supported", "Velocity")
            ProxyMode.VELOCITY
        } else {
            console().sendLang("Plugin-Proxy-None")
            ProxyMode.NONE
        }
    }

    val processor by unsafeLazy {
        executor
        when (mode) {
            ProxyMode.BUNGEE -> {
                BukkitProxyProcessor.BungeeSide()
            }
            ProxyMode.VELOCITY -> {
                BukkitProxyProcessor.VelocitySide()
            }
            ProxyMode.REDIS -> {
                RedisManager()
                BukkitProxyProcessor.RedisSide()
            }
            else -> null
        }
    }

    override fun close() {
        processor?.close()
        executor.shutdownNow()
    }

    override fun getPlayerNames(): Map<String, String?> {
        return allPlayerNames
    }

    fun getPlayerNamesMerged(): Set<String> {
        return allPlayerNames.let { it.keys + it.values.filterNotNull() }
    }

    override fun getExactName(name: String): String? {
        var player = Bukkit.getPlayerExact(name)
        if (player == null) {
            player = Bukkit.getOnlinePlayers().firstOrNull { ChatColor.stripColor(it.displayName) == name }
        }
        return if (player != null && player.isOnline) {
            player.name
        } else {
            getPlayerNames().entries.firstOrNull {
                it.key.equals(name, ignoreCase = true) || it.value?.equals(name, ignoreCase = true) == true
            }?.key
        }
    }

    override fun isPlayerOnline(name: String): Boolean {
        return getExactName(name) != null
    }

    override fun sendMessage(recipient: Any?, data: Array<String>): Future<*> {
        if (processor == null || recipient !is PluginMessageRecipient) return CompletableFuture.completedFuture(false)
        return processor!!.sendMessage(recipient, executor, data)
    }

    fun sendProxyLang(recipient: Any?, target: String, node: String, vararg args: String) {
        if (processor == null || Bukkit.getPlayerExact(target) != null) {
            getProxyPlayer(target)?.sendLang(node, *args)
        } else {
            sendMessage(recipient, arrayOf("ForwardMessage", "SendLang", target, node, *args))
        }
    }

    fun sendBroadcastRaw(recipient: Any?, uuid: UUID, component: ComponentText, listenPerm: String, doubleTransfer: Boolean, ports: List<Int>) {
        sendMessage(recipient, arrayOf(
            "BroadcastRaw",
            uuid.parseString(),
            component.toRawMessage(),
            listenPerm,
            doubleTransfer.toString(),
            ports.joinToString(";"))
        )
    }

    fun sendPrivateRaw(recipient: Any?, to: String, from: String, component: ComponentText) {
        sendMessage(recipient, arrayOf("ForwardMessage", "SendPrivateRaw", to, from, component.toRawMessage()))
    }

    fun updateNames() {
        sendMessage(onlinePlayers.firstOrNull(), arrayOf(
            "UpdateNames",
            onlinePlayers.filter { it.name !in PlayerData.vanishing }.joinToString(",") { it.name + "-" + ChatColor.stripColor(it.displayName) },
            port.toString()
        ))
    }

}