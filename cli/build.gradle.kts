plugins {
  `java-library`
}

dependencies {
  api(project(":core"))
  api("info.picocli:picocli:4.7.7")

  testImplementation("org.junit.jupiter:junit-jupiter:5.14.2")
}

