plugins {
    `kotlin-dsl`
}

group = "com.coffeepeek"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test-junit"))
}
