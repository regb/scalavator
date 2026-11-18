package com.regblanc.scalavator
package core

import sgl.proxy.{PlatformProxy, ProxiedGameApp, ProxySaveProvider, WiredProxyPlatform}

/** Platform wiring used by the generated Kotlin Android application. */
object Wiring {
  def wire(platform: PlatformProxy): ProxiedGameApp = new WiredProxyPlatform(platform)
    with AbstractApp
    with GeneratedAssetsProvider
    with ProxySaveProvider {

    override val SaveName: String = "scalavator-savefile"
    override val TargetFps: Option[Int] = Some(30)
  }
}
