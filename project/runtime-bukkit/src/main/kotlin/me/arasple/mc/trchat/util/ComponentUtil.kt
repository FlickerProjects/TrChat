package me.arasple.mc.trchat.util

import me.arasple.mc.trchat.TrChat
import me.arasple.mc.trchat.api.nms.NMS
import me.arasple.mc.trchat.module.adventure.hoverItemAdventure
import me.arasple.mc.trchat.util.color.colorify
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.chat.HoverEvent
import org.bukkit.Material
import org.bukkit.block.ShulkerBox
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BlockStateMeta
import org.bukkit.inventory.meta.ItemMeta
import taboolib.library.reflex.Reflex.Companion.getProperty
import taboolib.module.chat.ComponentText
import taboolib.module.chat.component
import taboolib.module.nms.MinecraftVersion.versionId
import taboolib.module.nms.NMSItemTag
import taboolib.module.nms.getI18nName
import taboolib.platform.Folia
import taboolib.platform.util.*

fun String.parseSimple() = component().build {
    transform { it.colorify() }
}

fun ComponentText.hoverItemFixed(item: ItemStack): ComponentText {
    if (Folia.isFolia) {
        return hoverItemAdventure(item)
    }
    var newItem = item.optimizeShulkerBox()
    newItem = NMS.instance.optimizeNBT(newItem)
    if (versionId >= 12005) {
        this.getProperty<ArrayList<BaseComponent>>("latest")!!.forEach {
            it.hoverEvent = HoverEvent(HoverEvent.Action.SHOW_ITEM, ComponentBuilder(NMSItemTag.instance.toMinecraftJson(newItem)).create())
        }
        return this
    }
    return try {
        // https://github.com/TrPlugins/TrChat/issues/363
        NMS.instance.hoverItem(this, newItem)
    } catch (_: Throwable) {
        try {
            hoverItem(newItem)
        } catch (_: Throwable) {
            hoverText("Unable to display this item! Click to view it.")
        }
    }
}

@Suppress("Deprecation")
fun ItemStack.optimizeShulkerBox(): ItemStack {
    if (!type.name.endsWith("SHULKER_BOX")) {
        return this
    }
    try {
        val itemClone = clone()
        val blockStateMeta = itemClone.itemMeta!! as BlockStateMeta
        val shulkerBox = blockStateMeta.blockState as ShulkerBox
        val contents = shulkerBox.inventory.contents
        val contentsClone = contents.mapNotNull {
            if (it.isNotAir()) {
                ItemStack(Material.STONE, it.amount, it.durability).modifyMeta<ItemMeta> {
                    if (it.itemMeta?.hasDisplayName() == true) {
                        setDisplayName(it.itemMeta!!.displayName)
                    } else {
                        setDisplayName(it.getI18nName())
                    }
                }
            } else {
                null
            }
        }.toTypedArray()
        shulkerBox.inventory.contents = contentsClone
        blockStateMeta.blockState = shulkerBox
        itemClone.itemMeta = blockStateMeta
        return itemClone
    } catch (_: Throwable) {
    }
    return this
}

@Suppress("Deprecation")
fun ItemStack.filter() {
    if (isAir()) return
    modifyMeta<ItemMeta> {
        if (hasDisplayName()) {
            setDisplayName(TrChat.api().getFilterManager().filter(displayName).filtered)
        }
        modifyLore {
            if (isNotEmpty()) {
                replaceAll { TrChat.api().getFilterManager().filter(it).filtered }
            }
        }
    }
}