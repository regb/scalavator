package com.regblanc.scalavator
package core

import sgl._

/** The platform-independent game. Each platform entry point mixes in its backend. */
trait AbstractApp extends Screen2DGameApp
  with LoadingScreenComponent
  with MainScreenComponent
  with AssetsProvider {
  this: SaveComponent =>
}
