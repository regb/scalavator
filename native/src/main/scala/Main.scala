package com.regblanc.scalavator
package native

import core.ScalavatorGame

import sgl.native._
import sgl.native.util._

object Main extends ScalavatorGame with NativeApp
  with TraceStdErrLoggingProvider {

  override val frameDimension = (400, 650)

}
