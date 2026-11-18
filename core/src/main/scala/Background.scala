package com.regblanc.scalavator
package core

import sgl._

import scala.util.Random

trait BackgroundComponent extends StyleComponent {
  this: CanvasProvider with WindowProvider =>

  import Graphics._

  /** Sky gradients (top, bottom) by altitude in dp: day, sunset, dusk, then space. */
  private val SkyStops: Vector[(Float, Rgb, Rgb)] = Vector(
    (0f, Rgb(86, 178, 255), Rgb(208, 242, 255)),
    (4000f, Rgb(72, 126, 226), Rgb(255, 210, 168)),
    (9000f, Rgb(54, 46, 138), Rgb(200, 108, 158)),
    (15000f, Rgb(10, 12, 38), Rgb(38, 32, 92))
  )

  private def skyColors(altitudeDp: Float): (Rgb, Rgb) = {
    val i = SkyStops.lastIndexWhere(_._1 <= altitudeDp).max(0)
    if(i == SkyStops.length - 1) (SkyStops(i)._2, SkyStops(i)._3)
    else {
      val (a0, top0, bottom0) = SkyStops(i)
      val (a1, top1, bottom1) = SkyStops(i + 1)
      val t = ((altitudeDp - a0) / (a1 - a0)).max(0f)
      (top0.lerp(top1, t), bottom0.lerp(bottom1, t))
    }
  }

  private val SkyBands = 48

  /** Fills the screen with the sky gradient for the given altitude. */
  protected def drawSky(canvas: Canvas, altitudeDp: Float): Unit = {
    val (top, bottom) = skyColors(altitudeDp)
    val bandHeight = screenHeight / SkyBands
    for(i <- 0 until SkyBands) {
      val color = top.lerp(bottom, i / (SkyBands - 1f)).color()
      canvas.drawRect(0f, i * bandHeight, screenWidth, bandHeight + 1, fill(color))
    }
  }

  private def smoothstep(from: Float, to: Float, x: Float): Float = {
    val t = ((x - from) / (to - from)).max(0f).min(1f)
    t * t * (3 - 2 * t)
  }

  /** An endless sky: gradient, stars when high enough, parallax clouds, and the starting hills.
    *
    * The background scrolls slower than the foreground to give some depth.
    */
  class Background(ground: Bitmap, cloudBitmaps: Vector[Bitmap]) {

    /** How far the foreground has scrolled up, in pixels. */
    private var scrolled: Float = 0f
    private var time: Long = 0

    def altitudeDp: Float = px2dp(scrolled)

    private final class Cloud(var x: Float, baseY: Float, val bitmap: Bitmap, val depth: Float, val speed: Float) {
      val scale: Float = 0.55f + depth
      def y: Float = baseY + scrolled * depth
    }

    private def newCloud(screenY: Float): Cloud = {
      val depth = 0.2f + Random.nextFloat() * 0.3f
      val bitmap = cloudBitmaps(Random.nextInt(cloudBitmaps.size))
      val x = Random.nextFloat() * (screenWidth + bitmap.width) - bitmap.width
      new Cloud(x, screenY - scrolled * depth, bitmap, depth, randomDp(6, 10))
    }

    private var clouds: List[Cloud] = List.tabulate(4)(i => newCloud(screenHeight * (0.1f + 0.2f * i)))
    private var nextCloudAt: Float = dp(120)

    def scroll(delta: Float): Unit = {
      scrolled += delta
      while(scrolled > nextCloudAt) {
        clouds ::= newCloud(-dp(120))
        // Clouds move at a fraction of the foreground speed, so space them widely.
        nextCloudAt += randomDp(380, 260)
      }
      clouds = clouds.filter(_.y < screenHeight)
    }

    def update(dt: Long): Unit = {
      time += dt
      for(cloud <- clouds) {
        cloud.x += cloud.speed * dt / 1000f
        if(cloud.x > screenWidth) cloud.x = -cloud.bitmap.width * cloud.scale
      }
    }

    private val StarColor = Rgb(255, 250, 230)
    private val stars: Vector[(Float, Float, Float)] =
      Vector.fill(70)((Random.nextFloat(), Random.nextFloat(), 1f + Random.nextFloat() * 1.5f))

    def render(canvas: Canvas): Unit = {
      val altitude = altitudeDp
      drawSky(canvas, altitude)

      val starAlpha = smoothstep(8000f, 14000f, altitude)
      if(starAlpha > 0) {
        for(((sx, sy, size), i) <- stars.zipWithIndex) {
          val twinkle = 0.65f + 0.35f * scala.math.sin(time / 400.0 + i).toFloat
          val y = (sy * screenHeight + scrolled * 0.05f) % screenHeight
          canvas.drawCircle(sx * screenWidth, y, dp(size), fill(StarColor.color(starAlpha * twinkle)))
        }
      }

      // The hills are a bit wider than the screen and sit just behind the starting platform.
      val groundScale = screenWidth / ground.width * 1.4f
      val groundTop = screenHeight - dp(6) - ground.height * groundScale + scrolled * 0.5f
      if(groundTop < screenHeight)
        canvas.drawBitmap(ground, (screenWidth - ground.width * groundScale) / 2, groundTop, groundScale)

      val cloudAlpha = 0.9f * (1f - 0.7f * smoothstep(10000f, 15000f, altitude))
      for(cloud <- clouds.sortBy(_.depth))
        canvas.drawBitmap(cloud.bitmap, cloud.x, cloud.y, cloud.scale, cloudAlpha)
    }
  }
}
