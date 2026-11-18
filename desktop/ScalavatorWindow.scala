package com.regblanc.scalavator.desktop

import sgl.{CanvasProvider, SystemProvider}
import sgl.awt.AWTWindowProvider
import sgl.util.LoggingProvider

/** Names the desktop window. */
trait ScalavatorWindow extends AWTWindowProvider {
  this: CanvasProvider with SystemProvider with LoggingProvider =>

  override val frameTitle: String = "Scalavator"
}
