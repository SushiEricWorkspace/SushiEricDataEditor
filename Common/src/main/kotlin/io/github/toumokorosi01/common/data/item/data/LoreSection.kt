package io.github.toumokorosi01.common.data.item.data

import io.github.toumokorosi01.common.stats.player.StatsType
import io.github.toumokorosi01.common.data.core.DeepCopyable
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting
import org.spongepowered.configurate.serialize.TypeSerializer
import java.lang.reflect.Type

object LoreSectionSerializer : TypeSerializer<LoreSection> {

    override fun deserialize(
        type: Type,
        node: ConfigurationNode
    ): LoreSection {
        val sectionType = node.node("type").get(LoreSectionType::class.java)
            ?: LoreSectionType.PLAIN_TEXT

        return when (sectionType) {
            LoreSectionType.PLAIN_TEXT -> PlainTextLoreSection(
                text = node.node("text").getString("empty"),
                secret = node.node("secret").getBoolean(false)
            )

            LoreSectionType.STATS -> StatLoreSection(
                stat = node.node("stat").get(StatsType::class.java)
                    ?: StatsType.MAX_HEALTH
            )

            LoreSectionType.CUSTOM_COMPONENT -> CustomComponentLoreSection(
                component = node.node("component").get(Component::class.java)
                    ?: Component.text("empty")
                        .color(NamedTextColor.WHITE)
                        .decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE)
            )
        }
    }

    override fun serialize(
        type: Type,
        obj: LoreSection?,
        node: ConfigurationNode
    ) {
        if (obj == null) {
            node.raw(null)
            return
        }

        node.node("type").set(LoreSectionType::class.java, obj.type)

        when (obj) {
            is PlainTextLoreSection -> {
                node.node("text").set(obj.text)
                node.node("secret").set(obj.secret)
            }

            is StatLoreSection -> {
                node.node("stat").set(StatsType::class.java, obj.stat)
            }

            is CustomComponentLoreSection -> {
                node.node("component").set(Component::class.java, obj.component)
            }
        }
    }
}

sealed interface LoreSection : DeepCopyable<LoreSection> {
    val type: LoreSectionType

    fun toComponent(): Component
}

enum class LoreSectionType(val display: String) {
    PLAIN_TEXT("通常テキスト"),
    CUSTOM_COMPONENT("カスタムComponent"),
    STATS("ステータス")
}

@ConfigSerializable
data class PlainTextLoreSection(
    @Setting("text")
    var text: String = "empty",

    @Setting("secret")
    var secret: Boolean = false
) : LoreSection {

    override val type: LoreSectionType
        get() = LoreSectionType.PLAIN_TEXT

    override fun deepCopy(): PlainTextLoreSection {
        return this.copy()
    }

    override fun toComponent(): Component {
        return if (secret) {
            Component.text(text)
                .color(NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, true)
        } else {
            Component.text(text)
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false)
        }
    }
}

@ConfigSerializable
data class CustomComponentLoreSection(
    @Setting("component")
    var component: Component = Component.text("empty")
        .color(NamedTextColor.WHITE)
        .decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE)
) : LoreSection {

    override val type: LoreSectionType
        get() = LoreSectionType.CUSTOM_COMPONENT

    override fun deepCopy(): CustomComponentLoreSection {
        return this.copy(
            component = this.component
        )
    }

    override fun toComponent(): Component {
        return component
    }

    /**
     * このカスタムComponent Loreの表示テキストを取得します。
     *
     * #### 仕様:
     * - 色、太字、斜体などの装飾情報は含めず、実際に表示される文字列のみを返します。
     * - [Component.toString] ではなく [PlainTextComponentSerializer] を使うため、
     *   `TextComponentImpl{...}` のようなデバッグ文字列ではなく、純粋な本文を取得できます。
     *
     * @return このカスタムComponent Loreのプレーンテキスト。
     */
    fun getText(): String {
        return PlainTextComponentSerializer.plainText().serialize(component)
    }

    /**
     * このカスタムComponent Loreに直接設定されている色を取得します。
     *
     * #### 仕様:
     * - 戻り値は Adventure API の [TextColor] です。
     * - 色が直接設定されていない場合は `null` を返します。
     * - 親ComponentやMinecraft側のデフォルト表示色まで解決するわけではありません。
     *
     * @return 直接設定されている [TextColor]。未設定の場合は `null`。
     */
    fun getColor(): TextColor? {
        return component.color()
    }

    /**
     * このカスタムComponent Loreに直接設定されている色をHex文字列で取得します。
     *
     * #### 仕様:
     * - 色が設定されている場合は `"#ffffff"` のようなHex形式で返します。
     * - 色が未設定の場合は `null` を返します。
     * - [getColor] の結果を文字列化するための便利メソッドです。
     *
     * @return Hex形式の色文字列。未設定の場合は `null`。
     */
    fun getHexColor(): String? {
        return getColor()?.asHexString()
    }

    /**
     * このカスタムComponent Loreに設定されている指定デコレーションの状態を取得します。
     *
     * #### 仕様:
     * - [TextDecoration.State.TRUE] は明示的に有効。
     * - [TextDecoration.State.FALSE] は明示的に無効。
     * - [TextDecoration.State.NOT_SET] は未設定、つまり親や表示側のデフォルトに委ねる状態です。
     *
     * @param decoration 取得したい装飾の種類。
     * @return 指定装飾の三値状態。
     */
    fun getDecoration(decoration: TextDecoration): TextDecoration.State {
        return component.decoration(decoration)
    }

    /**
     * このカスタムComponent Loreで、指定デコレーションが表示上有効かどうかをBooleanで取得します。
     *
     * #### 仕様:
     * - TRUE は true。
     * - FALSE は false。
     * - NOT_SET はアイテムLoreのデフォルト値として判定します。
     *
     * @param decoration 取得したい装飾の種類。
     * @return 表示上有効なら true。
     */
    fun isDecorationEnabledForItemLore(decoration: TextDecoration): Boolean {
        return when (getDecoration(decoration)) {
            TextDecoration.State.TRUE -> true
            TextDecoration.State.FALSE -> false
            TextDecoration.State.NOT_SET -> getItemLoreDefaultDecoration(decoration)
        }
    }

    /**
     * このカスタムComponent Loreで、指定デコレーションが明示的に有効かどうかを取得します。
     *
     * #### 注意:
     * - `TRUE` の場合のみ `true` を返します。
     * - `FALSE` と `NOT_SET` はどちらも `false` になります。
     * - `NOT_SET` は「無効」ではなく「未設定」なので、厳密に扱いたい場合は [getDecoration] を使ってください。
     *
     * @param decoration 判定したい装飾の種類。
     * @return 明示的に有効なら `true`。
     */
    fun hasDecoration(decoration: TextDecoration): Boolean {
        return getDecoration(decoration) == TextDecoration.State.TRUE
    }

    /**
     * このカスタムComponent Loreが太字に設定されているかを取得します。
     *
     * @return [TextDecoration.BOLD] が明示的に `TRUE` の場合は `true`。
     */
    fun isBold(): Boolean {
        return hasDecoration(TextDecoration.BOLD)
    }

    /**
     * このカスタムComponent Loreが斜体に設定されているかを取得します。
     *
     * #### 注意:
     * MinecraftのアイテムLoreは、表示側の仕様で斜体に見える場合があります。
     * このメソッドはComponentに明示された [TextDecoration.ITALIC] の状態だけを見ます。
     *
     * @return [TextDecoration.ITALIC] が明示的に `TRUE` の場合は `true`。
     */
    fun isItalic(): Boolean {
        return hasDecoration(TextDecoration.ITALIC)
    }

    /**
     * このカスタムComponent Loreが下線付きに設定されているかを取得します。
     *
     * @return [TextDecoration.UNDERLINED] が明示的に `TRUE` の場合は `true`。
     */
    fun isUnderlined(): Boolean {
        return hasDecoration(TextDecoration.UNDERLINED)
    }

    /**
     * このカスタムComponent Loreが打ち消し線付きに設定されているかを取得します。
     *
     * @return [TextDecoration.STRIKETHROUGH] が明示的に `TRUE` の場合は `true`。
     */
    fun isStrikethrough(): Boolean {
        return hasDecoration(TextDecoration.STRIKETHROUGH)
    }

    /**
     * このカスタムComponent Loreが難読化文字に設定されているかを取得します。
     *
     * @return [TextDecoration.OBFUSCATED] が明示的に `TRUE` の場合は `true`。
     */
    fun isObfuscated(): Boolean {
        return hasDecoration(TextDecoration.OBFUSCATED)
    }

    /**
     * このカスタムComponent Loreの「テキスト（表示文字列）」のみを書き換えます。
     *
     * #### 仕様:
     * - 元のComponentが持っていたスタイル（カラー、太字などの各種デコレーション）は、
     *   [Component.style] 経由で維持したまま、文字だけを差し替えます。
     * - childrenやtranslate/keybindなどのComponent構造は維持せず、単一のTextComponentとして再生成します。
     *
     * @param newText 新しく設定する文字列。
     */
    fun editText(newText: String) {
        component = Component.text(newText).style(component.style())
    }

    /**
     * このカスタムComponent Loreの「カラー（色）」のみを書き換えます。
     *
     * #### 仕様:
     * - テキスト内容や既存のデコレーションは維持したまま、色だけを変更します。
     * - [color] に `null` を渡した場合、明示的に設定されていた色情報を解除します。
     *
     * @param color 新しく設定したい [TextColor]。色をリセットしたい場合は `null`。
     */
    fun editColor(color: TextColor?) {
        applyColorChange(color)
    }

    /**
     * このカスタムComponent Loreの「カラー（色）」のみを書き換えます。
     *
     * #### 仕様:
     * - `"#FF5555"` や `"#55FF55"` などのHexカラーコード文字列を受け取り、
     *   自動的に [TextColor] に変換して適用します。
     * - テキスト内容や各種デコレーションは維持されます。
     *
     * @param colorHex 新しく設定したいHex形式の文字列。色をリセットしたい場合は `null`。
     */
    fun editColor(colorHex: String?) {
        val newColor = colorHex?.let { TextColor.fromHexString(it) }
        applyColorChange(newColor)
    }

    /**
     * [editColor] のオーバーロードから呼び出される、実際のカラー更新処理です。
     */
    private fun applyColorChange(newColor: TextColor?) {
        component = component.color(newColor)
    }

    /**
     * MinecraftのアイテムLoreにおける装飾のデフォルト値を返します。
     *
     * @param decoration 判定したい装飾の種類。
     * @return アイテムLore上でのデフォルト表示状態。
     */
    private fun getItemLoreDefaultDecoration(decoration: TextDecoration): Boolean {
        return when (decoration) {
            TextDecoration.ITALIC -> true
            else -> false
        }
    }

    /**
     * このカスタムComponent Loreの「デコレーション」を個別に切り替えます。
     *
     * #### 仕様:
     * - 設定済みのテキスト内容やカラー情報は維持されます。
     * - 各デコレーションの有効・無効は、Adventure APIの内部ステートへ変換されます。
     *
     * @param decoration 設定・操作したい装飾の種類。
     * @param state `true`で強制有効化、`false`で強制無効化。
     *              `null`を指定した場合は装飾の設定自体を削除し、親やデフォルトの挙動に委ねます。
     */
    fun editDecoration(decoration: TextDecoration, state: Boolean?) {
        val decorationState = when (state) {
            true -> TextDecoration.State.TRUE
            false -> TextDecoration.State.FALSE
            null -> TextDecoration.State.NOT_SET
        }

        component = component.decoration(decoration, decorationState)
    }
}

@ConfigSerializable
data class StatLoreSection(
    @Setting("stat")
    var stat: StatsType = StatsType.MAX_HEALTH
) : LoreSection {

    override val type: LoreSectionType
        get() = LoreSectionType.STATS

    override fun deepCopy(): StatLoreSection {
        return this.copy()
    }

    override fun toComponent(): Component {
        return Component.text(stat.display)
            .color(stat.color)
            .decoration(TextDecoration.ITALIC, false)
    }
}