package com.regblanc.scalavator
package core

import sgl._

/** Screen helpers, colors, and drawing shortcuts shared by every screen. */
trait StyleComponent {
  this: CanvasProvider with WindowProvider =>

  import Graphics._

  protected def screenWidth: Float = Window.width.toFloat
  protected def screenHeight: Float = Window.height.toFloat
  protected def safeTop: Float = Window.safeArea.top.toFloat
  protected def safeBottom: Float = Window.safeArea.bottom.toFloat

  /** Converts device-independent pixels to screen pixels. */
  protected def dp(value: Float): Float = Window.dp2px(value)
  /** Converts screen pixels to device-independent pixels. */
  protected def px2dp(value: Float): Float = value * 160f / Window.logicalPpi
  /** A random distance in `[min, min + range)` dp, converted to pixels. */
  protected def randomDp(min: Int, range: Int): Float = dp((min + scala.util.Random.nextInt(range)).toFloat)

  protected final case class Rgb(r: Int, g: Int, b: Int) {
    def lerp(that: Rgb, t: Float): Rgb = {
      def mix(a: Int, b: Int): Int = (a + (b - a) * t).round
      Rgb(mix(r, that.r), mix(g, that.g), mix(b, that.b))
    }
    def color(alpha: Float = 1f): Color =
      Color.rgba(r, g, b, (alpha.max(0f).min(1f) * 255).round)
  }

  protected val Ink = Rgb(28, 32, 64)
  protected val ScalaRed = Rgb(222, 52, 48)
  protected val Gold = Rgb(255, 196, 46)
  protected val White = Rgb(255, 255, 255)

  private val sharedPaint = defaultPaint

  protected def fill(color: Color): Paint = sharedPaint.withColor(color)

  protected def text(size: Float, color: Color, alignment: Alignments.Alignment = Alignments.Center): Paint =
    sharedPaint.withColor(color).withFont(Font.DefaultBold.withSize(dp(size).round)).withAlignment(alignment)

  /** Draws text with a soft drop shadow, so it reads on any sky color. */
  protected def drawShadowedString(canvas: Canvas, str: String, x: Float, y: Float, paint: Paint): Unit = {
    canvas.drawString(str, x, y + dp(2), paint.withColor(Ink.color(0.35f)))
    canvas.drawString(str, x, y, paint)
  }

  /** The canvas has no rounded rectangle, so assemble one from non-overlapping pieces
    * (overlaps would show as darker spots with a translucent paint).
    */
  protected def drawRoundRect(canvas: Canvas, x: Float, y: Float, w: Float, h: Float, radius: Float, paint: Paint): Unit = {
    val r = radius.min(w / 2).min(h / 2)
    canvas.drawRect(x + r, y, w - 2 * r, h, paint)
    canvas.drawRect(x, y + r, r, h - 2 * r, paint)
    canvas.drawRect(x + w - r, y + r, r, h - 2 * r, paint)
    for((cx, cy) <- Seq((x, y), (x + w - r, y), (x, y + h - r), (x + w - r, y + h - r))) {
      canvas.withSave {
        canvas.clipRect(cx, cy, r, r)
        canvas.drawCircle(x + r + (if(cx > x) w - 2 * r else 0f), y + r + (if(cy > y) h - 2 * r else 0f), r, paint)
      }
    }
  }

  /** A translucent rounded badge with centered text, for the HUD. */
  protected def drawPill(canvas: Canvas, x: Float, y: Float, width: Float, label: String, size: Float, color: Color): Unit = {
    val height = dp(36)
    drawRoundRect(canvas, x, y, width, height, height / 2, fill(Ink.color(0.35f)))
    canvas.drawString(label, x + width / 2, y + height / 2 + dp(size * 0.35f), text(size, color))
  }

  /** A white card with a soft shadow, horizontally centered, so text reads over the busy scene. */
  protected def drawCard(canvas: Canvas, y: Float, maxWidth: Float, height: Float, alpha: Float = 1f): Unit = {
    val width = (screenWidth - dp(40)).min(maxWidth)
    val x = (screenWidth - width) / 2
    drawRoundRect(canvas, x, y + dp(5), width, height, dp(24), fill(Ink.color(0.25f * alpha)))
    drawRoundRect(canvas, x, y, width, height, dp(24), fill(White.color(0.96f * alpha)))
  }
}
