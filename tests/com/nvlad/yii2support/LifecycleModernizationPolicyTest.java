package com.nvlad.yii2support;

import junit.framework.TestCase;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import java.util.regex.Pattern;

public class LifecycleModernizationPolicyTest extends TestCase {
    private static final Pattern STATIC_PROJECT_MAP = Pattern.compile(
            "static\\s+(?:final\\s+)?(?:[\\w.]+\\.)?Map\\s*<\\s*Project\\s*,"
    );

    public void testLegacyLifecycleAndApiUsagesCannotReturn() throws IOException {
        Path projectRoot = Path.of(System.getProperty("user.dir"));
        String productionSources = readTree(projectRoot.resolve("src"));
        String descriptor = Files.readString(projectRoot.resolve("resources/META-INF/plugin.xml"));
        String buildScript = Files.readString(projectRoot.resolve("build.gradle.kts"));

        assertFalse(productionSources.contains("ApplicationComponent"));
        assertFalse(productionSources.contains("ContentFactory.SERVICE"));
        assertFalse(productionSources.contains("ToolWindowManagerEx"));
        assertFalse(productionSources.contains("toolWindowRegistered("));
        assertFalse(productionSources.contains("ListPopupImpl"));
        assertFalse(productionSources.contains("DataSourceUiUtil"));
        assertFalse(productionSources.contains("ServiceManager.getService"));
        assertFalse(productionSources.contains("kotlin.reflect.jvm.internal"));
        assertFalse(productionSources.contains("@Storage(file"));
        assertFalse(productionSources.contains("getBaseDir()"));
        assertFalse(productionSources.contains("invokeAutoPopup("));
        assertFalse(productionSources.contains("findReferences(psiElement)"));
        assertFalse(productionSources.contains("getAllSubclasses("));
        assertFalse(STATIC_PROJECT_MAP.matcher(productionSources).find());

        assertFalse(descriptor.contains("<application-components>"));
        assertFalse(descriptor.contains("<errorHandler"));
        assertTrue(descriptor.contains(
                "<typedHandler implementation=\"com.nvlad.yii2support.completion.YiiCompletionAutoPopupHandler\"/>"
        ));
        assertFalse(buildScript.contains("io.sentry"));
    }

    public void testMigrationCommandsHaveConstructorOnlyLifecycle() throws IOException {
        Path projectRoot = Path.of(System.getProperty("user.dir"));
        String commandBase = Files.readString(projectRoot.resolve(
                "src/com/nvlad/yii2support/migrations/commands/CommandBase.java"
        ));
        String commandContext = Files.readString(projectRoot.resolve(
                "src/com/nvlad/yii2support/migrations/commands/CommandContext.java"
        ));

        assertTrue(commandContext.contains("public record CommandContext"));
        assertFalse(commandBase.contains("setConsoleView("));
        assertFalse(commandBase.contains("repaintComponent("));
        assertFalse(commandBase.contains("setApplication("));
        assertFalse(commandBase.contains("ScheduledExecutorService"));
        assertFalse(commandBase.contains("scheduleWithFixedDelay("));
    }

    public void testTwigApiIsIsolatedBehindOptionalDescriptor() throws IOException {
        Path projectRoot = Path.of(System.getProperty("user.dir"));
        String productionSources = readTree(projectRoot.resolve("src"));
        String buildScript = Files.readString(projectRoot.resolve("build.gradle.kts"));
        String twigDescriptor = Files.readString(projectRoot.resolve("resources/META-INF/twig.xml"));

        assertFalse("core classes must not link against the optional Twig plugin",
                productionSources.contains("com.jetbrains.twig"));
        assertFalse("Twig must not be present on the production or test compile classpath",
                buildScript.contains("\"com.jetbrains.twig\""));
        assertTrue("the optional descriptor must register the isolated Twig adapter",
                twigDescriptor.contains("com.nvlad.yii2support.views.twig.TwigViewFileTypeSupport"));
    }

    private static String readTree(Path root) throws IOException {
        StringBuilder result = new StringBuilder();
        try (Stream<Path> paths = Files.walk(root)) {
            for (Path path : (Iterable<Path>) paths.filter(Files::isRegularFile)::iterator) {
                result.append(Files.readString(path)).append('\n');
            }
        }
        return result.toString();
    }
}
