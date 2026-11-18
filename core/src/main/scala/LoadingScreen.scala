package com.regblanc.scalavator
package core

import sgl._
import sgl.assets.DrawableAsset
import sgl.util._

trait LoadingScreenComponent extends BackgroundComponent {
  this: CanvasProvider with GameStateComponent with WindowProvider with LoggingProvider
  with MainScreenComponent with AssetsProvider =>

  import Graphics._

  /** Every bitmap used by the game, fully loaded. */
  final case class Sprites(
    heroIdle: Bitmap,
    heroCrouch: Bitmap,
    heroJump: Bitmap,
    heroDizzy: Bitmap,
    platformLeft: Bitmap,
    platformMiddle: Bitmap,
    platformRight: Bitmap,
    bugFrames: Vector[Bitmap],
    clouds: Vector[Bitmap],
    ground: Bitmap
  )

  override def startingScreen: GameScreen = {
    val d = Assets.drawable
    def load(asset: DrawableAsset): Loader[Bitmap] = Graphics.loadImage(asset)

    val heroIdle = load(d.heroIdle)
    val heroCrouch = load(d.heroCrouch)
    val heroJump = load(d.heroJump)
    val heroDizzy = load(d.heroDizzy)
    val platformLeft = load(d.platformLeft)
    val platformMiddle = load(d.platformMiddle)
    val platformRight = load(d.platformRight)
    val bugFrames = Vector(load(d.bug1), load(d.bug2))
    val clouds = Vector(load(d.cloud1), load(d.cloud2), load(d.cloud3), load(d.cloud4))
    val ground = load(d.ground)

    val loaders = Vector(heroIdle, heroCrouch, heroJump, heroDizzy,
      platformLeft, platformMiddle, platformRight, ground) ++ bugFrames ++ clouds

    // Only called by the loading screen once every loader succeeded.
    def loaded(loader: Loader[Bitmap]): Bitmap = loader.value.get.get

    new ScalavatorLoadingScreen(loaders, () => Sprites(
      loaded(heroIdle), loaded(heroCrouch), loaded(heroJump), loaded(heroDizzy),
      loaded(platformLeft), loaded(platformMiddle), loaded(platformRight),
      bugFrames.map(loaded), clouds.map(loaded), loaded(ground)
    ))
  }

  private class ScalavatorLoadingScreen(loaders: Seq[Loader[Bitmap]], sprites: () => Sprites)
      extends LoadingScreen[Bitmap](loaders) {

    override protected val minDuration: Long = 600

    override def nextScreen: GameScreen = new MainScreen(sprites())

    override def render(canvas: Canvas): Unit = {
      drawSky(canvas, 0f)
      val cx = screenWidth / 2
      val cy = screenHeight * 0.42f
      drawShadowedString(canvas, "Scalavator", cx, cy, text(40, ScalaRed.color()))

      val barWidth = dp(180)
      val barHeight = dp(12)
      val barX = cx - barWidth / 2
      val barY = cy + dp(36)
      drawRoundRect(canvas, barX, barY, barWidth, barHeight, barHeight / 2, fill(Ink.color(0.25f)))
      val progress = percentageProgress.toFloat
      if(progress > 0)
        drawRoundRect(canvas, barX, barY, (barWidth * progress).max(barHeight), barHeight, barHeight / 2, fill(Color.White))
    }
  }
}
