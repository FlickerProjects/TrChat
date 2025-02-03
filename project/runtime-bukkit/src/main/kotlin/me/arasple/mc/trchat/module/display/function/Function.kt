package me.arasple.mc.trchat.module.display.function

import me.arasple.mc.trchat.api.event.TrChatReloadEvent
import me.arasple.mc.trchat.module.internal.script.Reaction
import org.bukkit.entity.Player
import taboolib.common.io.runningClassesWithoutLibrary
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.ProxyPlayer
import taboolib.common.platform.function.adaptPlayer
import taboolib.common.platform.function.severe
import taboolib.common.util.VariableReader
import taboolib.common.util.replaceWithOrder
import taboolib.module.chat.ComponentText
import taboolib.module.chat.Components
import taboolib.module.lang.*

abstract class Function(val id: String) {

    open val alias = id

    open val reaction: Reaction? = null

    abstract fun createVariable(sender: Player, message: String): String

    abstract fun parseVariable(sender: Player, arg: String): ComponentText?

    abstract fun canUse(sender: Player): Boolean

    abstract fun checkCooldown(sender: Player, message: String): Boolean

    companion object {

        @JvmStatic
        val functions = mutableListOf<Function>()

        private val parser = VariableReader("[", "]")

        fun reload(customFunctions: List<CustomFunction>) {
            functions.clear()
            functions.addAll(runningClassesWithoutLibrary
                .filter { it.hasAnnotation(StandardFunction::class.java) }
                .mapNotNull { it.getInstance() as? Function }
            )
            functions.addAll(customFunctions)
            TrChatReloadEvent.Function(functions).call()
        }

        fun Player.getComponentFromLang(
            node: String,
            vararg args: Any,
            processor: (TypeJson, Int, VariableReader.Part, ProxyPlayer) -> ComponentText = { type, i, part, sender ->
                Components.text(part.text.translate(sender).replaceWithOrder(*args)).applyStyle(type, part, i, sender, *args)
            }
        ): ComponentText? {
            val sender = adaptPlayer(this)
            val file = sender.getLocaleFile() ?: return null
            return when (val type = file.nodes[node].let { if (it is TypeList) it.list[0] else it }) {
                is TypeJson -> {
                    var i = 0
                    val component = Components.empty()
                    parser.readToFlatten(type.text!!.joinToString()).forEach { part ->
                        component.append(processor(type, i, part, sender))
                        if (part.isVariable) {
                            i++
                        }
                    }
                    component
                }
                is TypeText -> {
                    Components.text(type.asText(adaptPlayer(this), *args)!!)
                }
                else -> {
                    severe("Error language type for functions (Required TypeJson or TypeText)")
                    null
                }
            }
        }

        fun ComponentText.applyStyle(type: TypeJson, part: VariableReader.Part, i: Int, sender: ProxyPlayer, vararg args: Any): ComponentText {
            val extra = type.jsonArgs.getOrNull(i)
            if (extra != null) {
                if (extra.containsKey("hover")) {
                    hoverText(extra["hover"].toString().translate(sender).replaceWithOrder(*args))
                }
                if (extra.containsKey("command")) {
                    clickRunCommand(extra["command"].toString().translate(sender).replaceWithOrder(*args))
                }
                if (extra.containsKey("suggest")) {
                    clickSuggestCommand(extra["suggest"].toString().translate(sender).replaceWithOrder(*args))
                }
                if (extra.containsKey("insertion")) {
                    clickInsertText(extra["insertion"].toString().translate(sender).replaceWithOrder(*args))
                }
                if (extra.containsKey("copy")) {
                    clickCopyToClipboard(extra["copy"].toString().translate(sender).replaceWithOrder(*args))
                }
                if (extra.containsKey("file")) {
                    clickOpenFile(extra["file"].toString().translate(sender).replaceWithOrder(*args))
                }
                if (extra.containsKey("url")) {
                    clickOpenURL(extra["url"].toString().translate(sender).replaceWithOrder(*args))
                }
                if (extra.containsKey("font")) {
                    font(extra["font"].toString().translate(sender).replaceWithOrder(*args))
                }
            }
            return this
        }

        internal fun String.translate(sender: ProxyCommandSender): String {
            var s = this
            Language.textTransfer.forEach { s = it.translate(sender, s) }
            return s
        }
    }
}