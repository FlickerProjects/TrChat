package me.arasple.mc.trchat.module.internal.hook.impl

import dev.lone.itemsadder.api.FontImages.FontImageWrapper
import me.arasple.mc.trchat.module.internal.hook.HookAbstract
import org.bukkit.entity.Player
import taboolib.module.nms.MinecraftVersion.versionId

/**
 * @author ItsFlicker
 * @since 2022/2/5 22:30
 */
class HookItemsAdder : HookAbstract() {

    fun replaceFontImages(message: String, player: Player?): String {
        if (!isHooked) {
            return message
        }
        return try {
            if (player == null || versionId >= 12005) {
                FontImageWrapper.replaceFontImages(message)
            } else {
                FontImageWrapper.replaceFontImages(player, message)
            }
        } catch (_: Throwable) {
            message
        }
    }
}