// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.detekt)
}

dependencies {
    detektPlugins(libs.detekt.formatting)
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
