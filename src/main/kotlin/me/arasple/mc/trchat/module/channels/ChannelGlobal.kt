package me.arasple.mc.trchat.module.channels

import me.arasple.mc.trchat.module.bstats.Metrics
import me.arasple.mc.trchat.api.event.GlobalShoutEvent
import me.arasple.mc.trchat.module.chat.ChatFormats
import me.arasple.mc.trchat.module.chat.obj.ChatType
import me.arasple.mc.trchat.module.data.Users
import me.arasple.mc.trchat.util.Bungees
import net.md_5.bungee.chat.ComponentSerializer
import org.bukkit.entity.Player
import taboolib.common.platform.function.console

/**
 * @author Arasple
 * @date 2019/8/17 23:06
 */
object ChannelGlobal {

    fun execute(from: Player, message: String) {
        GlobalShoutEvent(message).run {
            if (call()) {
                val format = ChatFormats.getFormat(ChatType.GLOBAL, from)!!.apply(from, this.message)
                val raw = ComponentSerializer.toString(*format.componentsAll.toTypedArray())
                Bungees.sendBungeeData(from, "TrChat", "BroadcastRaw", raw)
                format.sendTo(console())
                Users.formatedMessage[from.uniqueId] = raw
                Metrics.increase(0)
            }
        }
    }
}