Scalavator
==========

Scalavator is the unofficial Scala game. It is a cross-platform, open-source,
arcade game where you must jump from platform to platform to reach higher and
higher (scores).

Scalavator is inspired by the classic [Doodle
Jump](https://wikipedia.org/wiki/Doodle_Jump), but its mecanisms have been
adapted to be enjoyable to play in non-mobile platforms such as the web or the
desktop. It kept the simplicity of the controls in order to be fully playable
on mobile as well.

Scalavator was started as a way to demonstrate how to use [the Scala Game
Library (SGL)](https://github.com/regb/scala-game-library), a cross-platform
game engine for the Scala programming language. It is fully open source and its
code is under the MIT license. The goal was to show a small, but complete game
implemented with SGL and taking advantage of the cross-platform features of the
library.

The current implementation supports three platforms: Android, HTML5, and
Desktop JVM. The core game logic is under the [core](/core) directory tree. The
platform-specific implementations consist of very simple sub-projects (under
[android](/android), [html5](/html5), and [desktop](/desktop), respectively),
with only a single file to define the correct dependencies. For example, here
is the only required [Android
code](/android/src/main/scala/MainActivity.scala), just a couple lines of code.

Scalavator is available to [try online](http://regblanc.com/games/scalavator)
from your browser, or to download for your Android device on [Google
Play](https://play.google.com/store/apps/details=?id=com.regblanc.scalavator).
I invite you to try both and spot the differences.

You can also build and run Scalavator yourself, just run the following command:
    
    sbt desktop/run

And it should start the Desktop JVM version of Scalavator. You can also build
an Android apk (after installing the Android dependencies) with `sbt
android/android:packageDebug)` and a javascript build with (after installing
scala.js dependencies) `sbt html5/fastOptJs`.

Not that anyone would ever need them, but just in case, all the art assets that
I provided for the game are released in the public domain. Some assets on this
repository might come from a third-party source and are thus only reusable from
their oiginal source (usually, under a creative common license).

The game is not quite finished yet. The goal is to evolve along with the
development of SGL. The game will be ported to as many platforms as possible,
most notably to iOS, and as an executable for Windows, OSx, and Linux. I also
plan to add support for Leaderboards, achievements, and analytics, and show how
these can be integrated over all these platforms in a shared Scala code base.
Graphics are what they are --- programmer art.. I don't plan to spend much
more time on them, but if you are of the artistic kind, I will appreciate
any contribution.

If you are interested in trying out SGL, this repository is a good starting
point with a working project. You can then modify the core game logic to go
into any direction you wish. If you do build a game with SGL, please reach out
to [the SGL project](https://github.com/regb/scala-game-library), or get in
touch directly with [me](http://regblanc.com/contact).

Credits
-------

    * Author, designer, artist, and programmer: [Régis Blanc](http://regblanc.com)
    * [Ce-gars-là](TODO), original concept from [Manohar Jonnalagedda](https://twitter.com/manojah_shanti) and [Nicolas Stucki](https://twitter.com/stucki_nicolas).
