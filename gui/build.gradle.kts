plugins {
  `java-library`
  id("org.openjfx.javafxplugin") version "0.1.0"
}

javafx {
  version = "21.0.7"
  modules = listOf("javafx.controls", "javafx.graphics", "javafx.fxml")
}

dependencies {
  api(project(":core"))
  implementation("com.fasterxml.jackson.core:jackson-databind:2.21.1")

  testImplementation("org.junit.jupiter:junit-jupiter:5.14.2")
}

