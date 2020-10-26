package com.regblanc.scalavator
package core

import sgl._
import sgl.geometry._
import sgl.util.Loader

trait ScalavatorGame extends ViewportComponent {
  this: GraphicsProvider
  with InputProvider
  with WindowProvider
  with SystemProvider
  with GameStateComponent =>

  object Character {
    val Width = 1.2f
    val Height = 1.78f
  }
  class Character(val idleBitmap: Graphics.Bitmap, val jumpBitmap: Graphics.Bitmap) {
    // Position stores the bottom-left point of the character.
    var position = Point(0, 0)
    var velocity = Vec(0, 0)
    var platform: Option[Platform] = None

    def jumping = platform.isEmpty

    private var jumpDuration = 0l

    private val idleFrame: Graphics.BitmapRegion = Graphics.BitmapRegion(idleBitmap)
    private val jumpFrames: Array[Graphics.BitmapRegion] =
      Graphics.BitmapRegion.split(jumpBitmap, 0, 0, idleBitmap.width, idleBitmap.height, 6, 1)
    private val jumpAnimation = new Graphics.Animation(100, jumpFrames, Graphics.Animation.Normal)

    def update(dt: Long): Unit = {
      if(jumping)
        jumpDuration += dt
      else
        jumpDuration = 0
    }

    def render(canvas: Graphics.Canvas): Unit = {
      val frame = if(jumping) jumpAnimation.currentFrame(jumpDuration) else idleFrame
      canvas.drawBitmap(frame, position.x, position.y - Character.Height, Character.Width, Character.Height, 1f)
    }
  }

  object Platform {
    val BaseSpeed = 3f
    val Height = 0.2f
  }
  class Platform(var x: Float, var y: Float, val width: Float, var speed: Float) {

    def right = x + width
    //private val region = BitmapRegion(platformBitmap, 0 max (platformBitmap.width-width)/2, 0, platformBitmap.width, platformBitmap.height)
 
    private val Paint = Graphics.defaultPaint.withColor(Graphics.Color.rgb(60, 34, 17))
    def render(canvas: Graphics.Canvas): Unit = {
      canvas.drawRect(x, y, width, Platform.Height, Paint)
      //canvas.drawRepeatedBitmap(region, x.toInt, y.toInt, width, platformBitmap.height)
    }
  }

  class MainScreen extends GameScreen {
    override val name: String = "main-screen"

    // The world height is based on the size of the character.
    private val WorldHeight: Float = 8*Character.Height
    // We set the width to maintain the aspect ratio of the screen.
    private val WorldWidth: Float = Window.width*(WorldHeight/Window.height)

    private val Gravity = Vec(0, 20)
    private val JumpImpulsion = Vec(0, -15)

    val viewport = new Viewport(Window.width, Window.height)
    viewport.setCamera(0, -WorldHeight, WorldWidth, WorldHeight)
    viewport.scalingStrategy = Viewport.Fit

    val characterIdleBitmap: Loader[Graphics.Bitmap] = Graphics.loadImage(MultiDPIResourcesRoot / "character_idle.png")
    addPreloading(characterIdleBitmap)
    val characterJumpBitmap: Loader[Graphics.Bitmap] = Graphics.loadImage(MultiDPIResourcesRoot / "character_jump.png")
    addPreloading(characterJumpBitmap)
 
    var character: Character = null

    override def onLoaded(): Unit = {
      character = new Character(characterIdleBitmap.value.get.get, characterJumpBitmap.value.get.get)
      /*
       * character position is the bottom left corner of the hittable area. The
       * actual visible sprite expands slightly more to the left and the right of
       * the CharacterWidth.
       */
      character.position = Point(WorldWidth/2 - Character.Width/2, 0)
    }

    def randomPlatform(index: Int): Platform = {
      val speed = if(index % 2 == 0) Platform.BaseSpeed else -Platform.BaseSpeed
      val y = -2*index - 4f
      val x = ((index*index) % 7)/7f * WorldWidth
      new Platform(x, y, 1.8f, speed)
    }

    import scala.collection.mutable.Queue
    private val platforms: Queue[Platform] = new Queue[Platform]
    platforms.enqueue(new Platform(0, 0, WorldWidth, 0))
    for(i <- 0 to 18)
      platforms.enqueue(randomPlatform(i))

    private var platformIndex = 19
    private def replacePlatform(): Unit = {
      platforms.dequeue()
      platforms.enqueue(randomPlatform(platformIndex))
      platformIndex += 1
    }

    // private var totalTime: Long = 0
    // private var chargeJumpStart: Long = 0

    def handleInput(ev: Input.InputEvent): Unit = ev match {
      // case Input.TouchDownEvent(_, _, _) | Input.MouseDownEvent(_, _, Input.MouseButtons.Left) =>
      //   //if(gameOver)
      //   //  restart()

      //   //if(standingPlatform.nonEmpty) {
      //     chargeJumpStart = totalTime
      //     //characterAnimation.currentAnimation = CharacterPreJumpAnimation
      //   //}
      case Input.TouchUpEvent(_, _, _) | Input.MouseUpEvent(_, _, Input.MouseButtons.Left) if !character.jumping =>
        // if(chargeJumpStart != 0) {// && !hitByBug && !freeFalling) {
          // val totalCharge = totalTime - chargeJumpStart
          // chargeJumpStart = 0
        // if(standingPlatform.nonEmpty) {
          character.velocity = JumpImpulsion
          character.platform = None
        // }
        
      case _ => ()
    }
    Input.setEventProcessor(handleInput)

    override def update(dt: Long): Unit = {
      // totalTime += dt
      val originalCharacterFeet = character.position.y
      character.velocity += Gravity*(dt/1000f)
      character.position += character.velocity*(dt/1000f)

      if(character.velocity.y > 0) {
        platforms.find(p =>
          originalCharacterFeet <= p.y && character.position.y >= p.y && 
          character.position.x + Character.Width >= p.x && character.position.x <= p.right
        ).foreach(platform => {
          character.velocity = Vec(0, 0)
          character.position = character.position.withY(platform.y)
          character.platform = Some(platform)
        })
      }

      if(character.position.y < viewport.cameraY + viewport.cameraHeight/2) {
        // We need to scroll the camera back to the center.
        viewport.translateCameraTo(0, character.position.y - viewport.cameraHeight/2)
        if(platforms.head.y > viewport.cameraY + viewport.cameraHeight) {
          replacePlatform()
        }
      }

      character.update(dt)

      for(platform <- platforms) {
        platform.x += platform.speed*(dt/1000f)
        character.platform.foreach(p => {
          if(p == platform) {
            character.position.x += platform.speed*(dt/1000f)
          }
        })
        if(platform.right > WorldWidth) {
          platform.x = WorldWidth - platform.width
          platform.speed = -platform.speed
        } else if(platform.x < 0) {
          platform.x = 0
          platform.speed = - platform.speed
        }
      }

      if(character.position.y > viewport.cameraY + viewport.cameraHeight)
        gameState.newScreen(new MainScreen)
    }

    private val BlueSky = Graphics.Color.rgb(181, 242, 251)
    override def render(canvas: Graphics.Canvas): Unit = {
      canvas.drawColor(BlueSky)
      viewport.withViewport(canvas){
        for(platform <- platforms)
          platform.render(canvas)
        character.render(canvas)
      }
    }
  }

  override def startingScreen: GameScreen = new MainScreen
}

//  private implicit val Tag = Logger.Tag("main")
//
//  class MainScreen(
//    characterIdleBitmap: Bitmap,
//    characterPreJumpBitmap: Bitmap,
//    characterJumpBitmap: Bitmap,
//    bugBitmap: Bitmap,
//    platformBitmap: Bitmap,
//    cloudsBitmap: Bitmap
//  ) extends GameScreen {
//
//    private def jumpChargeToImpulsion(jumpCharge: Long): Double = {
//      //let's try 3 layers, for each 200 ms
//      val level: Int = {
//        val tmp = (jumpCharge / 200d).toInt //for each 200ms, we have 1 level
//        tmp min 2
//      }
//      //level is from 0 to MaxLevel
//
//      (1 + 0.3*level)*dp2px(320)
//    }
//
//    class Bug(var x: Double, var y: Double, var speed: Double) {
//      private var age: Long = 0
//      def update(dt: Long): Unit = {
//        age += dt
//        x = x + speed*(dt/1000d)
//        if(x + Bug.Width > WindowWidth) {
//          x = WindowWidth - Bug.Width
//          speed = -speed
//        } else if(x < 0) {
//          x = 0
//          speed = -speed
//        }
//      }
//      def render(canvas: Canvas): Unit = {
//        val frame = if(speed > 0) bugRightAnimation.currentFrame(age) else bugLeftAnimation.currentFrame(age)
//        canvas.drawBitmap(frame, x.toInt, y.toInt)
//      }
//
//      // Character position (left-bottom of the hittable area)
//      def hitCharacter(characterX: Double, characterY: Double): Boolean = {
//        (characterX + CharacterHitboxStart >= x + dp2px(6) && characterX < x + Bug.Width - dp2px(6)) &&
//        (characterY >= y + dp2px(15) && characterY - CharacterIdleHeight < y + Bug.Height - dp2px(10))
//      }
//
//    }
//    object Bug {
//      val Width = dp2px(64)
//      val Height = dp2px(64)
//      def random(y: Double): Bug = {
//        val x = scala.util.Random.nextInt(WindowWidth - Width)
//        val speed = dp2px(90 + scala.util.Random.nextInt(20))
//        new Bug(x, y, speed)
//      }
//    }
//
//
//    private var randomNextPop: Int = 0
//    // Distance to jump until we pop the next bug
//    private var distanceToNextBug: Int = WindowHeight
//    private def generateRandomNextPop: Int = dp2px(70 + scala.util.Random.nextInt(30))
//    private def generateRandomDistanceToBug: Int = dp2px(WindowHeight/2 + WindowHeight)
//
//    private val startingPlatform = new Platform(0, WindowHeight-platformBitmap.height, WindowWidth, 0)
//    private var platforms: List[Platform] = List(startingPlatform)
//
//    {
//      var h = WindowHeight - dp2px(100)
//      while(h > 0) {
//        platforms ::= Platform.random(h)
//        h -= dp2px(100)
//      }
//      randomNextPop = -h
//    }
//
//    private var bugs: List[Bug] = List()
//    // private var bugs: List[Bug] = List(new Bug(0, WindowHeight - 200, 100))
//
//    //character real height varies from sprite to sprite, and the value
//    //refers to the sprite height (but when idle, it uses ony about 3/4 of
//    //that height). The width only refers to the inner part that is collidable
//    //and not the full sprite with the arms.
//    // The pixel at which the character becomes collidable
//    private val CharacterHitboxStart = dp2px(18)
//    // The width of the hitbox from the starting point
//    private val CharacterHitboxWidth = dp2px(24)
//    private val CharacterHeight = characterIdleBitmap.height //dp2px(89)
//    private val CharacterIdleHeight = dp2px(65)
//
//    private val characterIdleFrames = Array(
//      BitmapRegion(characterIdleBitmap, 0        ,  0, dp2px(60), CharacterHeight)
//    )
//
//    private val characterPreJumpFrames = Array(
//      BitmapRegion(characterPreJumpBitmap, 0         , 0, dp2px(60), CharacterHeight),
//      BitmapRegion(characterPreJumpBitmap, dp2px(60) , 0, dp2px(60), CharacterHeight),
//      BitmapRegion(characterPreJumpBitmap, dp2px(120), 0, dp2px(60), CharacterHeight)
//    )
//
//    private val characterLandingFrames = Array(
//      characterJumpFrames(1),
//      characterJumpFrames(0),
//      characterJumpFrames(1),
//      characterJumpFrames(2),
//      characterIdleFrames(0)
//    )
//
//    private val BugLeftFrames = Array(
//      BitmapRegion(bugBitmap, 0         , 0, Bug.Width, Bug.Height),
//      BitmapRegion(bugBitmap, dp2px(64) , 0, Bug.Width, Bug.Height),
//      BitmapRegion(bugBitmap, dp2px(128), 0, Bug.Width, Bug.Height)
//    )
//    private val BugRightFrames = Array(
//      BitmapRegion(bugBitmap, 0         , dp2px(64), Bug.Width, Bug.Height),
//      BitmapRegion(bugBitmap, dp2px(64) , dp2px(64), Bug.Width, Bug.Height),
//      BitmapRegion(bugBitmap, dp2px(128), dp2px(64), Bug.Width, Bug.Height)
//    )
//
//    private val CharacterIdleAnimation = new Animation(200, characterIdleFrames, Animation.Loop)
//    private val CharacterPreJumpAnimation = new Animation(100, characterPreJumpFrames, Animation.Normal)
//    private val CharacterStartJumpAnimation = new Animation(100, characterJumpFrames, Animation.Normal)
//    private val CharacterTopJumpAnimation = new Animation(200, characterJumpFrames.reverse.take(5), Animation.Normal)
//    private val CharacterLandingAnimation = new Animation(150, characterLandingFrames, Animation.Normal)
//    private val bugLeftAnimation = new Animation(200, BugLeftFrames, Animation.LoopReversed)
//    private val bugRightAnimation = new Animation(200, BugRightFrames, Animation.LoopReversed)
//
//
//    //this looks like a standard wrapper technique for a character/sprite
//    //that can have several state and thus several animation. It seems
//    //more convenient to have an internal shared elapsed time, that
//    //is reset each time the animation change, and properly updated 
//    //by a simple call to update, no matter what the current animation is.
//    //The alternative being to store global variables in the game logic,
//    //and tracking which current animation is going on to get the frame
//    //with the proper elapsed time.
//    //So maybe, this could be part of the library
//    class CharacterAnimation {
//      private var _currentAnimation: Animation = CharacterIdleAnimation
//      private var elapsed: Long = 0
//
//      def update(dt: Long) = elapsed += dt
//
//      def currentAnimation_=(animation: Animation): Unit = {
//        _currentAnimation = animation
//        elapsed = 0
//      }
//      def currentFrame = _currentAnimation.currentFrame(elapsed)
//    }
//
//    private var characterAnimation = new CharacterAnimation
//
//    private var standingPlatform: Option[Platform] = Some(startingPlatform)
//
//    // The score is how high we go, it's a long, just in case
//    var currentScore: Double = 0
//    private var highestScore: Long = 0
//
//    private val hud = new Hud(this)
//
//    private var hitByBug = false
//    private var freeFalling = false
//    private var scrollDownVelocity = 0d
//    private var scrolledDown = 0d
//
//    private var gameOver = false
//
//    def handleInput(ev: Input.InputEvent): Unit = {
//      ev match {
//        case Input.TouchDownEvent(_, _, _) | Input.MouseDownEvent(_, _, Input.MouseButtons.Left) =>
//          if(gameOver)
//            restart()
//
//          if(standingPlatform.nonEmpty) {
//            chargeJumpStart = totalTime
//            characterAnimation.currentAnimation = CharacterPreJumpAnimation
//          }
//        case Input.TouchUpEvent(_, _, _) | Input.MouseUpEvent(_, _, Input.MouseButtons.Left) =>
//          if(chargeJumpStart != 0 && !hitByBug && !freeFalling) {
//            val totalCharge = totalTime - chargeJumpStart
//            chargeJumpStart = 0
//            logger.info("Jump input from player detected. total charge: " + totalCharge)
//            if(standingPlatform.nonEmpty) {
//              standingPlatform = None
//              characterVelocity = Vec(0, - jumpChargeToImpulsion(totalCharge))
//              characterAnimation.currentAnimation = CharacterStartJumpAnimation
//            }
//          }
//        case _ => ()
//      }
//    }
//
//    private val background = new Background(cloudsBitmap)
//
//    private var accumulatedDelta = 0l
//    private val FixedDelta = 5l
//    override def update(dt: Long): Unit = {
//      Input.processEvents(handleInput)
//
//      accumulatedDelta += dt
//
//      while(accumulatedDelta / FixedDelta != 0) {
//        accumulatedDelta -= FixedDelta
//        fixedUpdate(FixedDelta)
//      }
//
//      characterAnimation.update(dt)
//
//      background.update(dt)
//    }
//
//    def fixedUpdate(dt: Long): Unit = {
//      hud.sceneGraph.update(dt)
//
//      val originalCharacterFeet = characterPosition.y
//      platforms.foreach(_.update(dt))
//      bugs.foreach(_.update(dt))
//  
//      if(gameOver) {
//        // wait for a touch event to restart.
//      } else if(freeFalling) {
//        characterVelocity += Gravity*(dt/1000d)
//        characterPosition += characterVelocity*(dt/1000d)
//        if(characterPosition.y.toInt - WindowHeight > 0) {
//          scrollDownVelocity = 2*characterVelocity.y
//        }
//        if(characterPosition.y > WindowHeight + CharacterHeight) {
//          gameOver = true
//          highestScore = save.getLongOrElse("highest_score", 0)
//          if(currentScore.toInt > highestScore) {
//            highestScore = currentScore.toInt
//            save.putLong("highest_score", highestScore)
//          }
//        } else if(scrolledDown < WindowHeight) {
//          val scrollDownDistance = scrollDownVelocity*(dt/1000d)
//          scrollDown(scrollDownDistance)
//          scrolledDown += scrollDownDistance
//        }
//      } else {
//        standingPlatform match {
//          case None => {
//            val previousVelocity = characterVelocity
//
//            characterVelocity += Gravity*(dt/1000d)
//            characterPosition += characterVelocity*(dt/1000d)
//
//            //TODO: maybe we should start an animation just before reaching the top,
//            //      but we need to create State within the character to properly handle
//            //      the different phase of the jump
//            if(characterVelocity.y >= -dp2px(50)) { //trying to detect end of the jump
//            }
//
//            if(previousVelocity.y <= 0 && characterVelocity.y >= 0) {
//              //if reached peak of the jump
//              //characterAnimation.currentAnimation = CharacterEndJumpAnimation
//              characterAnimation.currentAnimation = CharacterTopJumpAnimation
//            }
//
//          }
//          case Some(platform) => {
//            characterPosition = characterPosition + Vec(1,0)*platform.speed*(dt/1000d)
//            if(characterPosition.x < 0)
//              characterPosition = characterPosition.copy(x = 0)
//            if(characterPosition.x + CharacterHitboxWidth > WindowWidth)
//              characterPosition = characterPosition.copy(x = WindowWidth-CharacterHitboxWidth)
//          }
//        }
//        val newCharacterFeet = characterPosition.y
//        if(newCharacterFeet > originalCharacterFeet) { //if falling
//          platforms.find(p => p.y+1 > originalCharacterFeet && p.y+1 <= newCharacterFeet && 
//                              p.x <= characterPosition.x + CharacterHitboxWidth && p.x + p.width >= characterPosition.x
//                        ).foreach(platform => {
//            standingPlatform = Some(platform)
//            characterAnimation.currentAnimation = CharacterLandingAnimation
//          })
//
//          if(standingPlatform == None && characterPosition.y > WindowHeight) {
//            freeFalling = true
//          }
//        }
//        if(bugs.exists(_.hitCharacter(characterPosition.x, characterPosition.y))) {
//          characterVelocity = Vec(0, dp2px(10))
//          freeFalling = true
//          hitByBug = true
//        }
//      }
//
//    }
//
//    def restart(): Unit = {
//      gameState.newScreen(
//        new MainScreen(
//          characterIdleBitmap, characterPreJumpBitmap, characterJumpBitmap,
//          bugBitmap, platformBitmap, cloudsBitmap
//        )
//      )
//    }
//
//    private val gameOverPaint = defaultPaint.withColor(Color.Black).withFont(Font.Default.withSize(dp2px(20))).withAlignment(Alignments.Center)
//    override def render(canvas: Canvas): Unit = {
//
//      background.render(canvas)
//
//      platforms.foreach(_.render(canvas))
//      bugs.foreach(_.render(canvas))
//
//      if(hitByBug) {
//        canvas.withSave {
//          canvas.translate((characterPosition.x.toInt - CharacterHitboxStart) + characterIdleBitmap.width/2, characterPosition.y.toInt - CharacterHeight/2)
//          //-dp2px(9), characterPosition.y.toInt-CharacterHeight)
//          canvas.rotate(math.Pi)
//          canvas.drawBitmap(characterAnimation.currentFrame, -characterIdleBitmap.width/2, -CharacterHeight/2)
//        }
//      } else {
//        canvas.drawBitmap(characterAnimation.currentFrame,
//          characterPosition.x.toInt- CharacterHitboxStart, characterPosition.y.toInt-CharacterHeight)
//      }
//
//      hud.sceneGraph.render(canvas)
//
//      if(gameOver) {
//        canvas.drawString("Score: " + currentScore.toInt, WindowWidth/2, WindowHeight/2 - dp2px(14), gameOverPaint)
//        canvas.drawString("Highest Score: " + highestScore, WindowWidth/2, WindowHeight/2 + dp2px(14), gameOverPaint)
//
//        if((totalTime/700d).toInt % 2 == 0)
//          canvas.drawString("Press to start a new game", WindowWidth/2, WindowHeight*3/4, gameOverPaint)
//      }
//
//    }
//
//    /*
//     * When jumping passed half the screen, we scroll up to maintain the character
//     * in the middle. We never scroll down, as essentially everything outside of the
//     * screen disappeared.
//     *
//     * We don't use a camera, we just shift everything down a bit, and drop the platform
//     * when it goes off screen.
//     */
//    private def scrollUp(distance: Int): Unit = {
//      platforms.foreach(plat => plat.y += distance)
//      bugs.foreach(bug => bug.y += distance)
//      characterPosition = characterPosition + Vec(0, distance.toDouble)
//
//      // Since we base the score on the distance (in actual pixels), we
//      // need to convert that distance back to dp, otherwise people
//      // with high density screens would get a much higher score while
//      // jumping the same distance.
//      currentScore += px2dp(distance)
//
//      randomNextPop -= distance
//      if(randomNextPop <= 0) {
//        randomNextPop = generateRandomNextPop
//        platforms ::= Platform.random(0)
//      }
//
//      distanceToNextBug -= distance
//      if(distanceToNextBug <= 0) {
//        distanceToNextBug = generateRandomDistanceToBug
//        bugs ::= Bug.random(-Bug.Height)
//      }
//
//      platforms = platforms.filterNot(p => p.y > WindowHeight)
//      bugs = bugs.filterNot(b => b.y > WindowHeight)
//
//      //paralax scrolling with background
//      background.scrollUp(distance/3d)
//    }
//
//    // scroll down is the inverse of scroll up, except that it will not generate
//    // any new platforms or enemies. We use it for our game over animation.
//    private def scrollDown(distance: Double): Unit = {
//      platforms.foreach(plat => plat.y -= distance)
//      bugs.foreach(bug => bug.y -= distance)
//      characterPosition = characterPosition - Vec(0, distance)
//    }
//  }
//
//
//  class Hud(mainScreen: MainScreen) {
//    val viewport = new Viewport(WindowWidth, WindowHeight)
//    val sceneGraph = new SceneGraph(WindowWidth, WindowHeight, viewport)
//
//    private val group = new SceneGroup(0, 0, WindowWidth, dp2px(40))
//    private val groupBackground = new GroupBackground
//    private val titleLabel = new TitleLabel
//    val scoreLabel = new ScoreLabel
//    group.addNode(groupBackground)
//    group.addNode(titleLabel)
//    group.addNode(scoreLabel)
//    sceneGraph.addNode(group)
//
//    private val textPaint = defaultPaint.withColor(Color.White).withFont(Font.Default.withSize(dp2px(18)))
//
//    class GroupBackground extends SceneNode(0, 0, 0, 0) {
//      override def update(dt: Long): Unit = {}
//      override def render(canvas: Canvas): Unit = {
//        canvas.drawColor(Color.Red)
//      }
//    }
//    class TitleLabel extends SceneNode(dp2px(15), dp2px(25), 0, 0) {
//      override def update(dt: Long): Unit = {}
//      override def render(canvas: Canvas): Unit = {
//        canvas.drawString("Scalavator", x.toInt, y.toInt, textPaint)
//      }
//    }
//    class ScoreLabel extends SceneNode(WindowWidth-dp2px(15), dp2px(25), 0, 0) {
//      override def update(dt: Long): Unit = {}
//      override def render(canvas: Canvas): Unit = {
//        canvas.drawString(mainScreen.currentScore.toInt.toString, x.toInt, y.toInt, textPaint.withAlignment(Alignments.Right))
//      }
//    }
//    
//  }
//
//}
