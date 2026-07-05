plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

tasks.register("checkCiSdkVersions") {
    val workflowFile = layout.projectDirectory.file(".github/workflows/android.yml")
    val expectedPlatform = providers.provider {
        "platforms;android-${libs.versions.androidCompileSdk.get()}.${libs.versions.androidCompileSdkMinor.get()}"
    }
    val expectedBuildTools = providers.provider {
        "build-tools;${libs.versions.androidBuildTools.get()}"
    }

    inputs.file(workflowFile)
    inputs.property("expectedPlatform", expectedPlatform)
    inputs.property("expectedBuildTools", expectedBuildTools)

    doLast {
        val workflow = workflowFile.asFile.readText()
        check(expectedPlatform.get() in workflow) {
            "GitHub Actions Android platform must match ${expectedPlatform.get()} from gradle/libs.versions.toml"
        }
        check(expectedBuildTools.get() in workflow) {
            "GitHub Actions build tools must match ${expectedBuildTools.get()} from gradle/libs.versions.toml"
        }
    }
}
