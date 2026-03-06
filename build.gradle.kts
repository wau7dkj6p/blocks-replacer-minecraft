import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
  base
}

allprojects {
  group = "com.blockreplace"
  version = "0.1.0-SNAPSHOT"
}

subprojects {
  repositories {
    mavenCentral()
  }
}

val javaVersion = 21

subprojects {
  plugins.withId("java") {
    the<JavaPluginExtension>().toolchain {
      languageVersion.set(JavaLanguageVersion.of(javaVersion))
    }
  }

  plugins.withId("java-library") {
    the<JavaPluginExtension>().toolchain {
      languageVersion.set(JavaLanguageVersion.of(javaVersion))
    }
  }

  tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging {
      exceptionFormat = TestExceptionFormat.FULL
    }
  }
}

