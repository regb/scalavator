load("@rules_scala//scala:scala.bzl", "scala_library")
load("@sgl//bazel/sgl:assets.bzl", "sgl_drawable_assets")
load("@sgl//bazel/sgl:defs.bzl", "sgl_android_app", "sgl_core_library", "sgl_desktop_awt_app", "sgl_html5_app")

# Art is drawn at xxhdpi (3x) and downscaled for lower densities.
sgl_drawable_assets(
    name = "drawables",
    srcs = glob(["assets/drawable-xxhdpi/*.png"]),
    source_density = "xxhdpi",
    strip_prefix = "assets/drawable-xxhdpi",
    target_densities = [
        "mdpi",
        "hdpi",
        "xhdpi",
        "xxhdpi",
    ],
)

sgl_core_library(
    name = "core",
    srcs = glob(["core/src/main/scala/**/*.scala"]),
    assets = [":drawables"],
    package = "com.regblanc.scalavator.core",
    deps = ["@sgl//engines:sgl-screen2d"],
)

scala_library(
    name = "desktop-window",
    srcs = ["desktop/ScalavatorWindow.scala"],
    deps = [
        "@sgl//backends/desktop-awt:sgl-desktop-awt",
        "@sgl//core:sgl-core",
    ],
)

sgl_desktop_awt_app(
    name = "desktop-awt",
    core_abstract_class = "com.regblanc.scalavator.core.AbstractApp",
    extra_mixins = [
        "com.regblanc.scalavator.core.GeneratedAssetsProvider",
        "com.regblanc.scalavator.desktop.ScalavatorWindow",
    ],
    file_save = "scalavator.save",
    frame_height = 650,
    frame_width = 400,
    main_class = "Main",
    package = "com.regblanc.scalavator",
    deps = [
        ":core",
        ":desktop-window",
    ],
)

sgl_html5_app(
    name = "html5",
    assets = [":drawables"],
    assets_strip_prefix = "assets",
    canvas_height = 650,
    canvas_width = 400,
    core_abstract_class = "com.regblanc.scalavator.core.AbstractApp",
    extra_mixins = ["com.regblanc.scalavator.core.GeneratedAssetsProvider"],
    main_class = "Scalavator",
    package = "com.regblanc.scalavator",
    deps = [":core"],
)

sgl_android_app(
    name = "android",
    android_resources_dir = "android/src/main/res",
    assets = [":drawables"],
    label = "Scalavator",
    launcher_icon = "@mipmap/ic_launcher",
    package = "com.regblanc.scalavator",
    version_code = 2,
    version_name = "1.0",
)
