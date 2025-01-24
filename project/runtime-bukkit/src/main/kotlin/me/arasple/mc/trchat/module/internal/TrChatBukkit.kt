package me.arasple.mc.trchat.module.internal

import me.arasple.mc.trchat.TrChat
import me.arasple.mc.trchat.api.impl.BukkitProxyManager
import me.arasple.mc.trchat.api.nms.NMS
import me.arasple.mc.trchat.module.conf.file.Filters
import me.arasple.mc.trchat.module.conf.file.Functions
import me.arasple.mc.trchat.module.conf.file.Settings
import me.arasple.mc.trchat.module.display.ChatSession
import me.arasple.mc.trchat.module.display.channel.Channel
import me.arasple.mc.trchat.module.display.function.Function
import me.arasple.mc.trchat.module.internal.data.PlayerData
import me.arasple.mc.trchat.module.internal.hook.HookPlugin
import org.bukkit.Bukkit
import taboolib.common.LifeCycle
import taboolib.common.platform.*
import taboolib.common.platform.function.console
import taboolib.common.platform.function.pluginVersion
import taboolib.module.lang.sendLang
import taboolib.module.nms.MinecraftVersion.majorLegacy

@PlatformSide(Platform.BUKKIT)
object TrChatBukkit : Plugin() {

    var isPaperEnv = false
        private set

    var isGlobalMuting = false

    internal fun detectPaperEnv() {
        try {
            // Paper 1.16.5+
            Class.forName("com.destroystokyo.paper.PaperConfig")
            if (majorLegacy >= 11604) {
                isPaperEnv = true
            }
        } catch (_: ClassNotFoundException) {
        }
    }

    @Awake(LifeCycle.CONST)
    internal fun onConst() {
//        System.setProperty("taboolib.dev", "true")
        detectPaperEnv()
//        registerLifeCycleTask(LifeCycle.INIT, 0) {
//            YamlUpdater.update("settings.yml", updateExists = false)
//        }
    }

    override fun onLoad() {
        console().sendLang("Plugin-Loading", Bukkit.getBukkitVersion())
    }

    override fun onEnable() {
//        if (!Settings.usePackets
//            || Folia.isFolia
//            || Bukkit.getPluginManager().isPluginEnabled("Geyser-Spigot")
//            || majorLegacy >= 12005
//            ) disablePacketListener()
        NMS.instance
        BukkitProxyManager.processor
        HookPlugin.printInfo()
        reload(console())
        console().sendLang("Plugin-Enabled", pluginVersion)
    }

    override fun onDisable() {
        BukkitProxyManager.close()

        ChatSession.sessions.clear()
        PlayerData.data.clear()
        Channel.channels.clear()
        Function.functions.clear()
    }

    fun reload(notify: ProxyCommandSender) {
        Settings.conf.reload()
        Functions.conf.reload()
        Filters.conf.reload()
        TrChat.api().getChannelManager().loadChannels(notify)
    }

}