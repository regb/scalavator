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

# The HTML5 game as a static website, in the layout the SGL dev server uses:
# the page and index.js at the root, the assets under static/.
genrule(
    name = "html5-site",
    srcs = [
        "html5/index.html",
        "html5/favicon.png",
        "marketing/banner.jpg",
        ":html5_indexjs",
        ":drawables",
    ],
    outs = ["html5-site.tar"],
    cmd = """
site=$$(mktemp -d)
cp $(location html5/index.html) $(location html5/favicon.png) $(location marketing/banner.jpg) $$site/
cp $(location :html5_indexjs) $$site/index.js
for f in $(locations :drawables); do
  path=$$site/static/$${f#*/assets/}
  mkdir -p $$(dirname $$path)
  cp $$f $$path
done
chmod -R u+w,a+r $$site
tar -C $$site --sort=name --mtime=@0 --owner=0 --group=0 -cf $@ .
rm -rf $$site
""",
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
