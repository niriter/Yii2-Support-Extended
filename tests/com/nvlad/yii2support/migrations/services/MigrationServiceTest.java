package com.nvlad.yii2support.migrations.services;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.nvlad.yii2support.common.YiiAlias;
import com.nvlad.yii2support.common.YiiApplicationProjectService;
import com.nvlad.yii2support.views.util.ViewPatternService;
import junit.framework.TestCase;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

public class MigrationServiceTest extends TestCase {
    public void testProjectStateClassesAreProjectServices() {
        assertProjectService(MigrationService.class);
        assertProjectService(YiiAlias.class);
        assertProjectService(YiiApplicationProjectService.class);
        assertProjectService(ViewPatternService.class);
    }

    public void testDifferentProjectsReturnDifferentMigrationServices() {
        Project firstProject = createProjectProxy("first");
        Project secondProject = createProjectProxy("second");

        assertSame(MigrationService.getInstance(firstProject), MigrationService.getInstance(firstProject));
        assertSame(MigrationService.getInstance(secondProject), MigrationService.getInstance(secondProject));
        assertNotSame(MigrationService.getInstance(firstProject), MigrationService.getInstance(secondProject));
    }

    public void testListenerIsRemovedWithParentDisposable() {
        MigrationService service = new MigrationService(createProjectProxy("listeners"));
        Disposable parent = Disposer.newDisposable("migration service test");

        service.addListener(() -> { }, parent);
        assertEquals(1, service.getListenerCountForTests());

        Disposer.dispose(parent);
        assertEquals(0, service.getListenerCountForTests());
    }

    private static void assertProjectService(Class<?> serviceClass) {
        Service annotation = serviceClass.getAnnotation(Service.class);
        assertNotNull(serviceClass.getName() + " must be a light service", annotation);
        assertTrue(Arrays.asList(annotation.value()).contains(Service.Level.PROJECT));
    }

    private static Project createProjectProxy(String name) {
        AtomicReference<MigrationService> migrationService = new AtomicReference<>();
        return (Project) Proxy.newProxyInstance(
                Project.class.getClassLoader(),
                new Class<?>[]{Project.class},
                (proxy, method, arguments) -> {
                    switch (method.getName()) {
                        case "getService":
                            if (arguments != null && arguments.length == 1
                                    && arguments[0] == MigrationService.class) {
                                MigrationService current = migrationService.get();
                                if (current == null) {
                                    current = new MigrationService((Project) proxy);
                                    migrationService.compareAndSet(null, current);
                                }
                                return migrationService.get();
                            }
                            return null;
                        case "isDisposed":
                            return false;
                        case "getName":
                        case "toString":
                            return name;
                        case "hashCode":
                            return System.identityHashCode(proxy);
                        case "equals":
                            return proxy == arguments[0];
                        default:
                            return defaultValue(method.getReturnType());
                    }
                }
        );
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == char.class) {
            return '\0';
        }
        return 0;
    }
}
