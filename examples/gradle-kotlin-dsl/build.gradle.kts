plugins {
    id("org.cqfn.diktat.diktat-gradle-plugin") version "0.4.2"
}

repositories {
    mavenCentral()
}

diktat {
    inputs = files("src/**/*.kt")
}
