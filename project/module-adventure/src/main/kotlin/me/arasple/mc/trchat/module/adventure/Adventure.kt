package me.arasple.mc.trchat.module.adventure

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.inventory.ItemStack
import taboolib.module.chat.ComponentText
import taboolib.module.chat.Components

private val legacySerializer: Any? = try {
    LegacyComponentSerializer.legacySection()
} catch (_: Throwable) {
    null
}

private val gsonSerializer: Any? = try {
    GsonComponentSerializer.gson()
} catch (_: Throwable) {
    null
}

fun gson(component: Component) = (gsonSerializer as GsonComponentSerializer).serialize(component)

fun gson(string: String) = (gsonSerializer as GsonComponentSerializer).deserialize(string)

fun Component.toNative() = Components.parseRaw(gson(this))

fun ComponentText.toAdventure() = gson(toRawMessage())

fun ComponentText.hoverItemAdventure(item: ItemStack): ComponentText {
    return toAdventure().hoverEvent(item.asHoverEvent()).toNative()
}