plugins {
  `java-library`
}

dependencies {
  api("com.fasterxml.jackson.core:jackson-databind:2.21.1")

  implementation("at.yawk.lz4:lz4-java:1.8.1")

  testImplementation("org.junit.jupiter:junit-jupiter-api:5.14.2")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.14.2")
}

