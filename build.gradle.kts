plugins {
    `java-library`
    id("maven-publish")
    id("com.github.johnrengelman.shadow").version("6.1.0")
}

repositories {
    jcenter()
    //Fetch confluent packages
    maven {
        url = uri("http://packages.confluent.io/maven/")
    }
}
group = "com.mercell.kafka.connect"

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
dependencies {
    implementation("org.apache.kafka:kafka-clients:2.6.0")
    implementation("org.apache.kafka:connect-api:2.6.0")
    implementation("org.postgresql:postgresql:42.2.15")
    implementation("io.confluent:kafka-connect-jdbc:5.5.2")
    api("com.datamountaineer:kafka-connect-common:2.0.4")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.0")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.7.0")
    testImplementation("org.assertj:assertj-core:3.17.1")
    testImplementation("org.testcontainers:junit-jupiter:1.14.3")
    testImplementation("org.testcontainers:postgresql:1.14.3")
    testImplementation("com.github.javafaker:javafaker:1.0.2")
    testRuntimeOnly("org.apache.calcite:calcite-avatica:1.6.0")
    testRuntimeOnly("org.slf4j:slf4j-simple:1.7.30")
    testImplementation("org.easymock:easymock:4.2")
}


val test by tasks.getting(Test::class) {
    // Use junit platform for unit tests
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        showExceptions = true
        showStackTraces = true
        showCauses = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStandardStreams = true
    }
}

val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

val githubUser: String? by project
val githubPassword: String? by project

publishing {
    repositories {
        maven {
            url = uri("https://maven.pkg.github.com/mercell/complex-types-postgres-jdbc-dialect")
            credentials {
                username = githubUser
                password = githubPassword
            }
        }
    }
    publications {
        create<MavenPublication>("mavenJava") {

            pom {
                name.set("complex-types-postgres-jdbc-dialect")
                description.set("Postgres JDBC dialect implementation with support for Complex types (STRUCT) in Kafka Connect")
                url.set("https://github.com/mercell/complex-types-postgres-jdbc-dialect")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }

                scm {
                    connection.set("scm:git:https://github.com/mercell/complex-types-postgres-jdbc-dialect.git")
                    developerConnection.set("scm:git:git@github.com:mercell/complex-types-postgres-jdbc-dialect.git")
                    url.set("https://github.com/mercell/complex-types-postgres-jdbc-dialect")
                }
            }
            from(components["java"])
            artifact(sourcesJar.get())
        }
    }
}