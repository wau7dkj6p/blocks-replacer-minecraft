plugins {
  application
  id("com.gradleup.shadow") version "9.3.2"
  id("org.openjfx.javafxplugin") version "0.1.0"
}

javafx {
  version = "21.0.7"
  modules = listOf("javafx.controls", "javafx.graphics", "javafx.fxml")
}

dependencies {
  implementation(project(":core"))
  implementation(project(":cli"))
  implementation(project(":gui"))
}

application {
  mainClass.set("com.blockreplace.app.Main")
}

tasks.shadowJar {
  archiveClassifier.set("all")
  mergeServiceFiles()
}

tasks.register<Exec>("jpackageImage") {
  group = "distribution"
  description = "Build Windows app-image (double-clickable .exe) via jpackage"
  dependsOn("shadowJar")

  val jarTask = tasks.named<Jar>("shadowJar")

  doFirst {
    val jarFile = jarTask.get().archiveFile.get().asFile
    val inputDir = jarFile.parentFile
    val destDir = layout.buildDirectory.dir("jpackage").get().asFile
    val appVersion = project.version.toString().substringBefore("-").ifBlank { "0.1.0" }
    val existing = destDir.resolve("block-replace")
    if (existing.exists()) {
      existing.deleteRecursively()
    }

    val args =
        mutableListOf(
            "jpackage",
            "--type",
            "app-image",
            "--name",
            "block-replace",
            "--app-version",
            appVersion,
            "--input",
            inputDir.absolutePath,
            "--main-jar",
            jarFile.name,
            "--main-class",
            application.mainClass.get(),
            "--dest",
            destDir.absolutePath)

    // Optional Windows icon for the generated .exe.
    // Prefer the GUI's block-replace.ico, fall back to app-local icon if present.
    val primaryIcon = rootProject.projectDir.resolve("gui/src/main/resources/icons/block-replace.ico")
    val fallbackIcon = projectDir.resolve("src/main/resources/icon/block-replace.ico")
    val iconFile =
        when {
          primaryIcon.isFile -> primaryIcon
          fallbackIcon.isFile -> fallbackIcon
          else -> null
        }
    if (iconFile != null && iconFile.isFile) {
      args.addAll(listOf("--icon", iconFile.absolutePath))
    }

    commandLine(args)
  }
}

tasks.register<Copy>("prepareRelease") {
  group = "distribution"
  description = "Prepare portable release folder with Windows app-image"
  dependsOn("jpackageImage")

  val sourceDir = layout.buildDirectory.dir("jpackage/block-replace").get().asFile
  val releaseDir = rootProject.layout.projectDirectory.dir("release/block-replace").asFile

  from(sourceDir)
  into(releaseDir)

  doLast {
    releaseDir.mkdirs()
    val readmeFile = releaseDir.resolve("README.txt")
    if (!readmeFile.isFile) {
      readmeFile.writeText(
          """
          block-replace release
          =====================

          This folder contains a Windows app-image built with jpackage.

          - To run the tool, double-click block-replace.exe.
          - You can move this folder anywhere; keep the contents together.

          GUI starts when launched without arguments.
          CLI starts when you pass arguments, for example:

            block-replace.exe --help
            block-replace.exe --level-dat "C:\path\to\world\level.dat" --from minecraft:snow --to air

          """.trimIndent())
    }
  }
}

