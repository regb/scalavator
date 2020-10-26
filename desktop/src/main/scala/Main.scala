package com.regblanc.scalavator
package desktop

import core.ScalavatorGame

import sgl.awt.AWTApp
import sgl.awt.util.VerboseStdOutLoggingProvider
//import sgl.scene._

object Main extends ScalavatorGame with AWTApp
  with VerboseStdOutLoggingProvider {

  //override val TargetFps = Some(60)

  override val frameDimension = (400, 650)

  //type Save = FileSave
  //override val save: Save = new FileSave("./scalavator.save")

}
