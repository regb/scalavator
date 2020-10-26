import sbtcrossproject.{crossProject, CrossType}

lazy val commonSettings = Seq(
  version        := "1.0",
  scalaVersion   := "2.12.11",
  scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")
)

val scalaNativeVer = "2.11.8"

lazy val core = (crossProject(JSPlatform, JVMPlatform, NativePlatform).crossType(CrossType.Pure) in file("./core"))
  .settings(commonSettings: _*)
  .settings(
    name := "scalavator-core",
    libraryDependencies += "com.regblanc.sgl" %%% "sgl-core" % "0.0.1"
  )
  .nativeSettings(scalaVersion := scalaNativeVer)

lazy val coreJVM = core.jvm
lazy val coreJS = core.js
lazy val coreNative = core.native

lazy val assets = file("assets")

lazy val desktop = (project in file("./desktop"))
  .settings(commonSettings: _*)
  .settings(
    name                := "scalavator-desktop",
    libraryDependencies += "com.regblanc.sgl" %% "sgl-desktop-awt" % "0.0.1",
    fork in run := true,
    unmanagedResourceDirectories in Compile := Seq(assets)
  )
  .dependsOn(coreJVM)

lazy val html5 = (project in file("./html5"))
  .enablePlugins(ScalaJSPlugin)
  .settings(commonSettings: _*)
  .settings(
    name := "scalavator-html5",
    libraryDependencies += "com.regblanc.sgl" %%% "sgl-html5" % "0.0.1",
    scalaJSUseMainModuleInitializer := true
  )
  .dependsOn(coreJS)

lazy val native = (project in file("./native"))
  .enablePlugins(ScalaNativePlugin)
  .settings(commonSettings: _*)
  .settings(scalaVersion := scalaNativeVer)
  .settings(
    name := "scalavator-native",
    libraryDependencies += "com.regblanc.sgl" %%% "sgl-desktop-native" % "0.0.1",
    unmanagedResourceDirectories in Compile := Seq(assets),
    // This only works on Linux, if you are running on Mac you
    // need these linking options instead:
    //    nativeLinkingOptions ++= Seq("-framework", "OpenGL")
    nativeLinkingOptions += "-lGL"
  )
  .dependsOn(coreNative)

//val scalaAndroidVer = "2.11.8"
//
//val commonAndroidSettings = Seq(
//    scalaVersion  := scalaAndroidVer,
//    scalacOptions += "-target:jvm-1.7",
//    javacOptions ++= Seq("-source", "1.7", "-target", "1.7"),
//    exportJars    := true
//)
//
//lazy val coreAndroid = (project in file("./core"))
//  .settings(commonSettings: _*)
//  .settings(commonAndroidSettings: _*)
//  .settings(
//    name         := "scalavator-core",
//    target       := baseDirectory.value / ".android" / "target"
//  )
//  .dependsOn(sglCoreAndroid)
//
//lazy val android = (project in file("./android"))
//  .enablePlugins(AndroidApp)
//  .settings(commonSettings: _*)
//  .settings(commonAndroidSettings: _*)
//  .settings(
//    name := "scalavator-android",
//    useProguard := true,
//    proguardOptions ++= Seq(
//        "-dontobfuscate",
//        "-dontoptimize",
//        "-keepattributes Signature",
//        "-dontwarn scala.collection.**", // required from Scala 2.11.3
//        "-dontwarn scala.collection.mutable.**", // required from Scala 2.11.0
//        "-dontwarn android.webkit.**", //required by adcolony
//        "-dontwarn com.immersion.**", //required by adcolony
//        "-dontnote com.immersion.**", //required by adcolony
//        "-ignorewarnings",
//        "-keep class scala.Dynamic",
//        "-keep class test.**"
//    ),
//    platformTarget := "android-23"
//  )
//  .dependsOn(sglCoreAndroid, sglAndroid, coreAndroid)
