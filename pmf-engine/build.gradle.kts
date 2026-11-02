plugins { java }
dependencies {
    api(project(":pmf-api"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
}
tasks.test {
    useJUnitPlatform()
}
