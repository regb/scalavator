package com.regblanc.scalavator
package core

import sgl._

import scala.util.Random

trait MainScreenComponent extends BackgroundComponent {
  this: CanvasProvider with GameStateComponent with WindowProvider
  with SaveComponent with LoadingScreenComponent =>

  import Graphics._

  private val HighScoreKey = "highest_score"

  private val StepMs = 5L

  /** Steps the physics every 5 ms for stable jumps, and catches up on at most 250 ms after a hitch. */
  class MainScreen(sprites: Sprites) extends FixedTimestepGameScreen(StepMs, maxStepsPerFrame = 50) {
    import sprites._

    override def name: String = "Scalavator"

    private val W = screenWidth
    private val H = screenHeight

    // Physics, in pixels and seconds.
    private val Gravity = dp(430)
    private val JumpImpulse = dp(320)
    private val ChargeStepMs = 200L
    private val MaxCharge = 2
    private val RestartDelayMs = 600L

    // The hero is positioned by the center of its feet; the hitbox ignores the arms.
    private val HeroHeight = heroIdle.height.toFloat
    private val HitboxWidth = heroIdle.width * 0.45f
    private val HitboxHeight = HeroHeight * 0.8f

    // The hero stands on the grass, slightly below the top of the platform bitmap.
    private val PlatformHeight = platformMiddle.height.toFloat
    private val PlatformSurface = PlatformHeight * 0.3f
    private val CapsWidth = (platformLeft.width + platformRight.width).toFloat
    private val middleRegion = BitmapRegion(platformMiddle, 0, 0, platformMiddle.width, platformMiddle.height)

    private val BugWidth = bugFrames.head.width.toFloat
    private val BugHeight = bugFrames.head.height.toFloat

    private def randomDirection: Float = if(Random.nextBoolean()) 1f else -1f

    /** Flies left and right, bouncing off the screen edges. */
    private trait Mover {
      var x: Float
      var y: Float
      var speed: Float
      def width: Float

      def update(dt: Float): Unit = {
        x += speed * dt
        if(x < 0 || x + width > W) {
          x = x.max(0f).min(W - width)
          speed = -speed
        }
      }
    }

    private final class Platform(var x: Float, var y: Float, val width: Float, var speed: Float) extends Mover {
      def render(canvas: Canvas): Unit = {
        val top = y - PlatformSurface
        if(width > CapsWidth)
          canvas.drawRepeatedBitmap(middleRegion, x + platformLeft.width, top, width - CapsWidth, PlatformHeight)
        canvas.drawBitmap(platformLeft, x, top)
        canvas.drawBitmap(platformRight, x + width - platformRight.width, top)
      }
    }

    private final class Bug(var x: Float, var y: Float, var speed: Float) extends Mover {
      val width: Float = BugWidth
      private var ageMs = 0L
      private def top: Float = y + dp(4) * scala.math.sin(ageMs / 180.0).toFloat

      override def update(dt: Float): Unit = {
        ageMs += (dt * 1000).toLong
        super.update(dt)
      }

      /** Checks overlap with a rectangle, ignoring the transparent margins of the sprite. */
      def hits(left: Float, top: Float, right: Float, bottom: Float): Boolean = {
        val inset = BugWidth * 0.2f
        right > x + inset && left < x + BugWidth - inset &&
        bottom > this.top + inset && top < this.top + BugHeight - inset
      }

      def render(canvas: Canvas): Unit = canvas.withSave {
        // The sprite faces right; mirror it when flying left.
        if(speed >= 0) canvas.translate(x, top)
        else { canvas.translate(x + BugWidth, top); canvas.scale(-1f, 1f) }
        canvas.drawBitmap(bugFrames(((ageMs / 90) % bugFrames.size).toInt), 0f, 0f)
      }
    }

    private enum Phase { case Ready, Playing, Falling, GameOver }

    private var phase = Phase.Ready
    private var time = 0L

    // The starting platform is the floor, along the bottom of the screen.
    private val startPlatform = new Platform(0, H - safeBottom - PlatformHeight * 0.7f, W, 0)
    private var platforms = List(startPlatform)
    private var topPlatformY = startPlatform.y
    private var bugs = List.empty[Bug]
    private var distanceToNextBug = H

    private var heroX = W / 2
    private var heroY = startPlatform.y
    private var heroVy = 0f
    private var standingOn: Option[Platform] = Some(startPlatform)
    private var chargingSince: Option[Long] = None
    private var landedAt: Option[Long] = None
    private var hitAt: Option[Long] = None
    private var scrolledDown = 0f

    private var climbedDp = 0f
    // Score in dp, so that high-density screens do not climb faster.
    private def score: Long = climbedDp.toLong
    private val previousBest = Save.getLongOrElse(HighScoreKey, 0)
    private var gameOverAt = 0L

    private val background = new Background(ground, clouds)

    private def heroLeft: Float = heroX - HitboxWidth / 2
    private def heroRight: Float = heroX + HitboxWidth / 2
    private def charge: Option[Int] = chargingSince.map(since => ((time - since) / ChargeStepMs).toInt.min(MaxCharge))

    private def fillPlatforms(): Unit = {
      while(topPlatformY > -dp(40)) {
        topPlatformY -= randomDp(70, 31)
        val width = randomDp(60, 40).max(CapsWidth)
        platforms ::= new Platform(Random.nextFloat() * (W - width), topPlatformY, width, randomDp(80, 60) * randomDirection)
      }
    }
    fillPlatforms()

    private def press(): Unit = phase match {
      case Phase.GameOver =>
        // Ignore the tap that killed the hero; give the player a moment to see the score.
        if(time - gameOverAt > RestartDelayMs) gameState.newScreen(new MainScreen(sprites))
      case Phase.Ready | Phase.Playing =>
        if(standingOn.nonEmpty && chargingSince.isEmpty) chargingSince = Some(time)
      case Phase.Falling => ()
    }

    private def release(): Unit = {
      for(level <- charge) {
        standingOn = None
        heroVy = -(1 + 0.3f * level) * JumpImpulse
        phase = Phase.Playing
      }
      chargingSince = None
    }

    Input.setInputProcessor(new InputProcessor with PointerInputProcessor {
      import Input.Keys.{Key, Space}
      import Input.MouseButtons.{Left, MouseButton}

      override def pointerDown(x: Int, y: Int, pointer: Int, button: MouseButton): Boolean = { if(button == Left) press(); true }
      override def pointerUp(x: Int, y: Int, pointer: Int, button: MouseButton): Boolean = { if(button == Left) release(); true }
      override def keyDown(key: Key): Boolean = { if(key == Space) press(); true }
      override def keyUp(key: Key): Boolean = { if(key == Space) release(); true }
    })

    override def fixedUpdate(): Unit = {
      time += StepMs
      step(StepMs / 1000f)
      background.update(StepMs)
    }

    private def fall(dt: Float): Unit = {
      heroVy += Gravity * dt
      heroY += heroVy * dt
    }

    private def step(dt: Float): Unit = {
      platforms.foreach(_.update(dt))
      bugs.foreach(_.update(dt))

      phase match {
        case Phase.GameOver => ()

        case Phase.Falling =>
          fall(dt)
          // Follow the fall for one screen, so the player sees the world rush by.
          val followLine = H * 0.6f
          if(heroY > followLine && scrolledDown < H) {
            val distance = (heroY - followLine).min(H - scrolledDown)
            scrollWorld(-distance)
            scrolledDown += distance
          }
          // Spinning makes the sprite extend past its height, so wait until it is well gone.
          if(heroY - HeroHeight * 1.5f > H) endGame()

        case Phase.Ready | Phase.Playing =>
          standingOn match {
            case Some(platform) =>
              heroX = (heroX + platform.speed * dt).max(HitboxWidth / 2).min(W - HitboxWidth / 2)
              heroY = platform.y
            case None =>
              val previousY = heroY
              fall(dt)
              if(heroVy > 0) {
                standingOn = platforms.find(p =>
                  p.y >= previousY && p.y <= heroY && heroRight >= p.x && heroLeft <= p.x + p.width)
                for(platform <- standingOn) {
                  heroY = platform.y
                  heroVy = 0
                  landedAt = Some(time)
                }
              }
              if(standingOn.isEmpty && heroY > H) phase = Phase.Falling
              val scrollLine = H * 0.45f
              if(heroY < scrollLine) scrollWorld(scrollLine - heroY)
          }

          if(bugs.exists(_.hits(heroLeft, heroY - HitboxHeight, heroRight, heroY))) {
            hitAt = Some(time)
            heroVy = -dp(160)
            standingOn = None
            chargingSince = None
            phase = Phase.Falling
          }
      }
    }

    /** Moves everything down by `distance` pixels (up when negative): the hero climbs, the world follows. */
    private def scrollWorld(distance: Float): Unit = {
      platforms.foreach(_.y += distance)
      bugs.foreach(_.y += distance)
      heroY += distance
      topPlatformY += distance
      background.scroll(distance)

      if(distance > 0) {
        climbedDp += px2dp(distance)
        fillPlatforms()
        distanceToNextBug -= distance
        if(distanceToNextBug <= 0) {
          distanceToNextBug = H * 1.5f
          bugs ::= new Bug(Random.nextFloat() * (W - BugWidth), -BugHeight, randomDp(90, 20) * randomDirection)
        }
        platforms = platforms.filter(_.y - PlatformSurface < H)
        bugs = bugs.filter(_.y < H)
      }
    }

    private def endGame(): Unit = {
      phase = Phase.GameOver
      gameOverAt = time
      if(score > previousBest) Save.putLong(HighScoreKey, score)
    }

    override def render(canvas: Canvas): Unit = {
      background.render(canvas)
      platforms.foreach(_.render(canvas))
      renderHero(canvas)
      bugs.foreach(_.render(canvas))
      phase match {
        case Phase.Ready => renderHud(canvas); renderTitle(canvas)
        case Phase.Playing | Phase.Falling => renderHud(canvas)
        case Phase.GameOver => renderGameOver(canvas)
      }
    }

    private val ChargeColors = Vector(Gold, Rgb(255, 140, 40), ScalaRed)

    private def renderHero(canvas: Canvas): Unit = {
      if(standingOn.nonEmpty)
        canvas.drawOval(heroX, heroY - dp(1), HitboxWidth * 1.4f, dp(7), fill(Ink.color(0.18f)))

      // Pick the pose, then squash and stretch it around the feet.
      val (bitmap, scaleX, scaleY) =
        if(hitAt.nonEmpty) (heroDizzy, 1f, 1f)
        else charge match {
          case Some(level) => (heroCrouch, 1f + 0.04f * level, 1f - 0.06f * level)
          case None if standingOn.isEmpty =>
            val stretch = (scala.math.abs(heroVy) / JumpImpulse * 0.08f).min(0.1f)
            (heroJump, 1f - stretch * 0.6f, 1f + stretch)
          case None if landedAt.exists(time - _ < 140) => (heroCrouch, 1.05f, 0.94f)
          case None =>
            val breathe = 0.015f * scala.math.sin(time / 320.0).toFloat
            (heroIdle, 1f - breathe, 1f + breathe)
        }

      canvas.withSave {
        canvas.translate(heroX, heroY)
        for(since <- hitAt) {
          canvas.translate(0f, -HeroHeight / 2)
          canvas.rotate((time - since) * 0.008f)
          canvas.translate(0f, HeroHeight / 2)
        }
        canvas.scale(scaleX, scaleY)
        canvas.drawBitmap(bitmap, -bitmap.width / 2f, -bitmap.height.toFloat)
      }

      for(level <- charge; i <- 0 to MaxCharge) {
        val cx = heroX + (i - 1) * dp(14)
        val cy = heroY - HeroHeight - dp(12)
        canvas.drawCircle(cx, cy, dp(5.5f), fill(Color.White))
        canvas.drawCircle(cx, cy, dp(4), fill(if(i <= level) ChargeColors(i).color() else Ink.color(0.15f)))
      }
    }

    private def pulse(periodMs: Double): Float = 0.6f + 0.4f * scala.math.sin(time * 2 * scala.math.Pi / periodMs).toFloat

    private def renderHud(canvas: Canvas): Unit = {
      val top = safeTop + dp(12)
      val margin = dp(12)
      drawPill(canvas, margin, top, dp(96), score.toString, 20, Color.White)
      drawPill(canvas, W - margin - dp(104), top, dp(104), s"BEST ${previousBest.max(score)}", 14, Gold.color())
    }

    private def renderTitle(canvas: Canvas): Unit = {
      val y = H * 0.24f
      drawCard(canvas, y, dp(320), dp(116))
      canvas.drawString("Scalavator", W / 2, y + dp(56), text(42, ScalaRed.color()))
      canvas.drawString("Hold to charge, release to jump", W / 2, y + dp(92), text(15, Ink.color(0.45f + 0.4f * pulse(1600))))
    }

    private def renderGameOver(canvas: Canvas): Unit = {
      val fadeIn = ((time - gameOverAt) / 300f).min(1f)
      canvas.drawRect(0f, 0f, W, H, fill(Ink.color(0.45f * fadeIn)))

      val height = dp(230)
      val y = (H - height) / 2 - dp(20) * (1 - fadeIn)
      val cx = W / 2
      drawCard(canvas, y, dp(300), height, fadeIn)

      canvas.drawString("Game Over", cx, y + dp(50), text(30, ScalaRed.color(fadeIn)))
      canvas.drawString("SCORE", cx, y + dp(84), text(13, Ink.color(0.5f * fadeIn)))
      canvas.drawString(score.toString, cx, y + dp(124), text(38, Ink.color(fadeIn)))

      if(score > previousBest) {
        val badgeWidth = dp(130)
        drawRoundRect(canvas, cx - badgeWidth / 2, y + dp(140), badgeWidth, dp(30), dp(15), fill(Gold.color(fadeIn)))
        canvas.drawString("NEW BEST!", cx, y + dp(161), text(15, Ink.color(fadeIn)))
      } else {
        canvas.drawString(s"BEST $previousBest", cx, y + dp(161), text(16, Ink.color(0.6f * fadeIn)))
      }

      if(time - gameOverAt > RestartDelayMs)
        canvas.drawString("Tap to play again", cx, y + dp(205), text(16, ScalaRed.color(pulse(1200))))
    }
  }
}
