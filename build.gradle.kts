import com.android.build.gradle.LibraryExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.jvm.tasks.Jar

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.detekt)
    alias(libs.plugins.dokka)
    alias(libs.plugins.dokka.javadoc) apply false
}

private val publishedLibraryModules = setOf(
    "scanner-core",
    "scanner-rules",
    "scanner-ui",
)

subprojects {
    if (name !in publishedLibraryModules) return@subprojects

    group = "io.github.mohdaquib"
    version = findProperty("VERSION_NAME") as? String
        ?: System.getenv("VERSION_NAME")
        ?: System.getenv("JITPACK_TAG")
        ?: "1.0.0"

    apply(plugin = "maven-publish")
    apply(plugin = "org.jetbrains.dokka")
    apply(plugin = "org.jetbrains.dokka-javadoc")

    plugins.withId("com.android.library") {
        extensions.configure<LibraryExtension>("android") {
            publishing {
                singleVariant("release") {
                    withSourcesJar()
                }
            }
        }

        val dokkaJavadocJar = tasks.register<Jar>("dokkaJavadocJar") {
            dependsOn("dokkaGeneratePublicationJavadoc")
            archiveClassifier.set("javadoc")
            from(layout.buildDirectory.dir("dokka/javadoc"))
        }

        afterEvaluate {
            extensions.configure<PublishingExtension>("publishing") {
                publications {
                    create<MavenPublication>("release") {
                        from(components["release"])
                        groupId = project.group.toString()
                        artifactId = project.name
                        version = project.version.toString()
                        artifact(dokkaJavadocJar)
                    }
                }
            }
        }
    }
}

dependencies {
    detektPlugins(libs.detekt.formatting)
    dokka(project(":scanner-core"))
    dokka(project(":scanner-rules"))
    dokka(project(":scanner-ui"))
}

detekt {
    source.setFrom(
        fileTree(rootDir) {
            include("**/src/**/*.kt")
            exclude("**/build/**", "**/.gradle/**")
        }
    )
    config.setFrom(file("config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
    parallel = true
}
