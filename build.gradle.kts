import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask

plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.18.1"
    id("idea")
}

val pluginVersion: String by project
val pluginSinceBuild: String by project
val pluginUntilBuild: String by project
val pluginVerifierIdeVersions: String by project
val platformVersion: String by project
val runPsiIntegrationTest = providers.gradleProperty("psiIntegrationTest")
    .map(String::toBoolean)
    .orElse(false)

val verifierIdeVersions = pluginVerifierIdeVersions
    .split(',')
    .map(String::trim)
    .filter(String::isNotEmpty)
    .map { it.removePrefix("PS-") }

group = "com.nvlad"
version = pluginVersion

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")

    intellijPlatform {
        phpstorm(platformVersion)
        bundledPlugins(
            "com.jetbrains.php",
            "com.intellij.database",
            "com.jetbrains.twig",
            "org.jetbrains.plugins.phpstorm-remote-interpreter",
            "org.jetbrains.plugins.terminal",
        )
        testFramework(TestFrameworkType.Bundled)
        pluginVerifier()
    }
}

sourceSets {
    main {
        java.srcDir("src")
        resources.srcDir("resources")
    }
    test {
        java.srcDir("tests")
        if (platformVersion == "2024.1.7") {
            java.srcDir("tests-psi")
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

intellijPlatform {
    pluginConfiguration {
        version = pluginVersion
        description = providers.fileContents(layout.projectDirectory.file("DESCRIPTION.md")).asText
        ideaVersion {
            sinceBuild = pluginSinceBuild
            untilBuild = pluginUntilBuild
        }
    }

    pluginVerification {
        // Existing internal API usage is tracked as technical debt; fail the build
        // on actual binary incompatibilities and invalid/missing dependencies.
        failureLevel = listOf(
            VerifyPluginTask.FailureLevel.COMPATIBILITY_PROBLEMS,
            VerifyPluginTask.FailureLevel.MISSING_DEPENDENCIES,
            VerifyPluginTask.FailureLevel.INVALID_PLUGIN,
        )
        ides {
            verifierIdeVersions.forEach {
                create(IntelliJPlatformType.PhpStorm, it)
            }
        }
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
        channels = listOf("beta")
    }
}

tasks {
    named<Zip>("buildPlugin") {
        archiveFileName = "yii2support.zip"
    }

    withType<JavaCompile>().configureEach {
        options.release = 17
        options.encoding = "UTF-8"
    }

    withType<Test>().configureEach {
        useJUnit()
        systemProperty("idea.load.plugins.id", "com.yii2support")
    }

    named<Test>("test") {
        if (runPsiIntegrationTest.get()) {
            filter {
                includeTestsMatching("com.nvlad.yii2support.objectfactory.ObjectFactoryContextPsiIntegrationTest")
            }
            reports.junitXml.outputLocation.set(layout.buildDirectory.dir("test-results/psiIntegrationTest"))
            reports.html.outputLocation.set(layout.buildDirectory.dir("reports/tests/psiIntegrationTest"))
        } else {
            exclude("**/*PsiIntegrationTest.class")
        }
    }

    register("runPluginVerifier") {
        group = "verification"
        description = "Compatibility alias for the IntelliJ Platform 1.x verifier task name."
        dependsOn("verifyPlugin")
    }
}
