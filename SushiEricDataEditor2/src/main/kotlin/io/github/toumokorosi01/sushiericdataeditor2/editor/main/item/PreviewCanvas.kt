package io.github.toumokorosi01.sushiericdataeditor2.editor.main.item

import io.github.toumokorosi01.common.data.item.data.ItemData
import javafx.scene.Group
import javafx.scene.SnapshotParameters
import javafx.scene.canvas.Canvas
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import javafx.scene.text.Text
import javafx.scene.transform.Shear
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer

/**
 * ItemDataの表示情報を画像として描画し、指定されたImageViewへ反映するプレビュー描画クラス。
 *
 * このクラスは、操作中のItemDataを参照し、表示名とLoreをCanvas上に描画して、
 * 生成した画像をImageViewへ設定する。
 *
 * 画像サイズは表示内容に合わせて自動で拡張される。
 * ただし、プレビュー領域が小さくなりすぎないように、最小幅と最小高さを持つ。
 *
 * Componentは1つのセクションとして扱い、Componentのchildrenは考慮しない。
 * 各セクションでは、文字色、太字、斜体、下線、取り消し線、難読化を反映する。
 *
 * 太字は同じ文字を1pxずらして追加描画することで表現する。
 * 斜体は文字を描画した画像にShear変形を適用して表現する。
 * 難読化は静止画用であり、アニメーションは行わない。
 *
 * @property itemData 現在操作しているItemDataの参照
 * @property imageView 生成したプレビュー画像を反映するImageView
 */
class PreviewCanvas(
    private val itemData: ItemData,
    private val imageView: ImageView
) {

    /**
     * 1行あたりの描画高さ。
     *
     * 表示名とLoreの各行を縦方向に配置するときの間隔として使用する。
     */
    private val lineHeight = 30.0

    /**
     * プレビュー内で使用する文字サイズ。
     *
     * 表示名、Lore、各セクションの文字描画に共通して使用する。
     */
    private val fontSize = 22.0

    /**
     * プレビュー描画に使用するフォントファミリー名。
     *
     * 実行環境で利用可能な日本語フォント候補から選択される。
     */
    private val fontFamily = findAvailableFontFamily()

    /**
     * プレビュー画像の最小幅。
     *
     * 内容が少ない場合でも、この幅より小さい画像にはしない。
     */
    private val minWidth = 280.0

    /**
     * プレビュー画像の最小高さ。
     *
     * 内容が少ない場合でも、この高さより小さい画像にはしない。
     */
    private val minHeight = 560.0

    /**
     * プレビュー画像内の左右余白。
     *
     * 文字が画像の左右端に密着しないようにするために使用する。
     */
    private val paddingX = 16.0

    /**
     * プレビュー画像内の上下余白。
     *
     * 文字が画像の上下端に密着しないようにするために使用する。
     */
    private val paddingY = 16.0

    /**
     * 1つのセクション画像と、次のセクションを描画するために進める横幅を保持する。
     *
     * 斜体などの変形後画像は実際の画像幅と見た目上の文字幅がずれるため、
     * Image.widthをそのまま使わず、advanceWidthを使って次の描画位置を決める。
     *
     * @property image 描画済みのセクション画像
     * @property advanceWidth 次のセクションを描画するために進める横幅
     */
    private data class SectionImage(
        val image: Image,
        val advanceWidth: Double,
        val offsetX: Double = 0.0
    )

    /**
     * 現在のItemDataの内容をもとにプレビュー画像を再生成し、ImageViewへ反映する。
     *
     * 外部から呼び出すための更新処理。
     * 表示名を1行目に描画し、その下にLoreを1行ずつ描画する。
     *
     * すべての行画像を先に生成し、その最大幅と行数から最終的な画像サイズを決定する。
     * 最終画像サイズは、内容に応じて拡張されるが、minWidthとminHeightより小さくならない。
     */
    fun refreshPreview() {
        val lineImages = mutableListOf<Image>()

        val nameLine = createLine(
            listOf(Component.text(itemData.display.displayName))
        )
        lineImages.add(nameLine)

        itemData.display.lore.forEach { loreLine ->
            val components = loreLine.map { section ->
                section.toComponent()
            }

            lineImages.add(createLine(components))
        }

        val contentWidth = lineImages
            .maxOfOrNull { it.width }
            ?: 0.0

        val contentHeight = lineImages.size * lineHeight

        val previewWidth = (contentWidth + paddingX * 2)
            .coerceAtLeast(minWidth)

        val previewHeight = (contentHeight + paddingY * 2)
            .coerceAtLeast(minHeight)

        val canvas = Canvas(previewWidth, previewHeight)
        val gc = canvas.graphicsContext2D

        gc.fill = Color.BLACK
        gc.fillRect(0.0, 0.0, previewWidth, previewHeight)

        var y = paddingY

        lineImages.forEach { lineImage ->
            gc.drawImage(lineImage, paddingX, y)
            y += lineHeight
        }

        imageView.image = canvas.snapshot(null, null)
    }

    /**
     * 1行分のComponentリストを横方向に連結し、1枚の行画像として生成する。
     *
     * List内の各Componentは1つのセクションとして扱う。
     * emptyListの場合は、文字のない空白行として透明なImageを返す。
     *
     * 各セクションの描画位置は、画像の実幅ではなくSectionImage.advanceWidthを使って進める。
     * これにより、斜体変形で画像に余白が増えた場合でも、セクション間隔が広がりすぎるのを防ぐ。
     *
     * @param components 1行分のセクションComponentリスト
     * @return 1行分を描画したImage
     */
    private fun createLine(components: List<Component>): Image {
        if (components.isEmpty()) {
            val emptyCanvas = Canvas(1.0, lineHeight)

            return emptyCanvas.snapshot(
                SnapshotParameters().apply {
                    fill = Color.TRANSPARENT
                },
                null
            )
        }

        val sections = components.map { component ->
            createSection(component)
        }

        val lineWidth = sections.sumOf { section ->
            section.advanceWidth
        }
            .plus(16.0) // 斜体の右側はみ出し用の余白
            .coerceAtLeast(1.0)

        val lineCanvas = Canvas(lineWidth, lineHeight)
        val lineGc = lineCanvas.graphicsContext2D

        var x = 0.0

        sections.forEach { section ->
            lineGc.drawImage(section.image, x + section.offsetX, 0.0)
            x += section.advanceWidth
        }

        return lineCanvas.snapshot(
            SnapshotParameters().apply {
                fill = Color.TRANSPARENT
            },
            null
        )
    }

    /**
     * 1つのComponentを1つのセクション画像として生成する。
     *
     * Component自身の文字、色、装飾のみを読み取り、childrenは考慮しない。
     * 対応する装飾は、難読化、太字、取り消し線、下線、斜体。
     *
     * 太字は文字を1px横にずらして追加描画することで表現する。
     * 斜体は通常描画後に画像全体をShearで歪ませて表現する。
     *
     * 戻り値には、生成画像だけでなく、次のセクションまで進める横幅も含める。
     * これは、斜体変形後の画像幅をそのまま使うと余白が広がりやすいため。
     *
     * @param component 描画対象のComponent
     * @return 1セクション分の画像と進行幅
     */
    private fun createSection(component: Component): SectionImage {
        val text = PlainTextComponentSerializer.plainText().serialize(component)

        val color = component.color()?.let {
            Color.web(it.asHexString())
        } ?: Color.WHITE

        val bold = component.decoration(TextDecoration.BOLD) == TextDecoration.State.TRUE
        val italic = component.decoration(TextDecoration.ITALIC) == TextDecoration.State.TRUE
        val underlined = component.decoration(TextDecoration.UNDERLINED) == TextDecoration.State.TRUE
        val strikethrough = component.decoration(TextDecoration.STRIKETHROUGH) == TextDecoration.State.TRUE
        val obfuscated = component.decoration(TextDecoration.OBFUSCATED) == TextDecoration.State.TRUE

        val displayText = if (obfuscated) {
            obfuscate(text)
        } else {
            text
        }

        val font = Font.font(fontFamily, FontWeight.NORMAL, fontSize)
        val textWidth = measureTextWidth(displayText, font)

        val italicPadding = if (italic) 6.0 else 0.0
        val sectionWidth = textWidth + italicPadding + 2.0

        val canvas = Canvas(sectionWidth, lineHeight)
        val gc = canvas.graphicsContext2D

        gc.font = font
        gc.fill = color

        val drawX = if (italic) 3.0 else 0.0
        val drawY = fontSize + 1.0

        gc.fillText(displayText, drawX, drawY)

        if (bold) {
            gc.fillText(displayText, drawX + 1.0, drawY)
        }

        if (underlined) {
            gc.stroke = color
            gc.lineWidth = 1.0
            gc.strokeLine(drawX, drawY + 3.0, drawX + textWidth, drawY + 3.0)
        }

        if (strikethrough) {
            gc.stroke = color
            gc.lineWidth = 1.0
            gc.strokeLine(drawX, drawY - fontSize * 0.35, drawX + textWidth, drawY - fontSize * 0.35)
        }

        val image = canvas.snapshot(
            SnapshotParameters().apply {
                fill = Color.TRANSPARENT
            },
            null
        )

        val resultImage = if (italic) {
            shearImage(image)
        } else {
            image
        }

        return SectionImage(
            image = resultImage,
            advanceWidth = textWidth + if (bold) 1.0 else 0.0,
            offsetX = if (italic) -4.0 else 0.0
        )
    }

    /**
     * 指定された画像を横方向に歪ませ、疑似的な斜体画像として返す。
     *
     * フォントに斜体が存在しない日本語文字でも、画像全体を変形することで
     * 斜体風に表示できる。
     *
     * @param image 歪ませる元画像
     * @return Shear変形を適用したImage
     */
    private fun shearImage(image: Image): Image {
        val imageView = ImageView(image)

        val group = Group(imageView).apply {
            transforms.add(Shear(-0.20, 0.0))
        }

        return group.snapshot(
            SnapshotParameters().apply {
                fill = Color.TRANSPARENT
            },
            null
        )
    }

    /**
     * Minecraftの難読化風に、空白以外の文字をランダムな文字へ置き換える。
     *
     * この処理は静止画用であり、アニメーションは行わない。
     *
     * @param text 難読化前の文字列
     * @return 難読化後の文字列
     */
    private fun obfuscate(text: String): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!?#$%&"

        return text.map { char ->
            if (char.isWhitespace()) {
                char
            } else {
                chars.random()
            }
        }.joinToString("")
    }

    /**
     * 指定されたフォントで描画した場合の文字列幅を取得する。
     *
     * セクション画像の横幅計算に使用する。
     *
     * @param text 幅を測定する文字列
     * @param font 測定に使用するフォント
     * @return 文字列の描画幅
     */
    private fun measureTextWidth(text: String, font: Font): Double {
        val helper = Text(text).apply {
            this.font = font
        }

        return helper.layoutBounds.width
    }

    /**
     * 使用可能な日本語フォントを環境から探して返す。
     *
     * Windows、macOS、Linux系で利用されやすい日本語フォントを候補として確認し、
     * 見つからなかった場合はJavaFXのデフォルトフォントを使用する。
     *
     * @return 使用するフォントファミリー名
     */
    private fun findAvailableFontFamily(): String {
        val availableFamilies = Font.getFamilies().toSet()

        val candidates = listOf(
            "Yu Gothic",
            "Yu Gothic UI",
            "Meiryo",
            "Hiragino Sans",
            "Hiragino Kaku Gothic ProN",
            "Apple SD Gothic Neo",
            "Noto Sans CJK JP",
            "Noto Sans JP"
        )

        return candidates.firstOrNull { it in availableFamilies }
            ?: Font.getDefault().family
    }
}