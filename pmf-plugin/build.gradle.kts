plugins { java; id("com.gradleup.shadow") }
dependencies {
 implementation(project(":pmf-api"));implementation(project(":pmf-engine"))
 compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
 implementation("org.xerial:sqlite-jdbc:3.50.3.0")
}
tasks {
 shadowJar { archiveClassifier.set("");relocate("org.sqlite","dev.pmf.libs.sqlite") }
 build { dependsOn(shadowJar) }
 jar { archiveClassifier.set("plain") }
}
