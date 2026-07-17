package com.nvlad.yii2support.migrations.planning;

import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.nvlad.yii2support.migrations.entities.Migration;
import com.nvlad.yii2support.migrations.entities.MigrationOperation;
import com.nvlad.yii2support.migrations.entities.MigrationStatus;
import junit.framework.TestCase;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MigrationSelectionPlannerTest extends TestCase {
    public void testOperationDefinesCliActionAndInitialDirection() {
        assertEquals("up", MigrationOperation.UP.getCommandAction());
        assertEquals("applying", MigrationOperation.UP.getInitialDirection());
        assertEquals("down", MigrationOperation.DOWN.getCommandAction());
        assertEquals("reverting", MigrationOperation.DOWN.getInitialDirection());
        assertEquals("redo", MigrationOperation.REDO.getCommandAction());
        assertEquals("reverting", MigrationOperation.REDO.getInitialDirection());
    }

    public void testPlanAllFiltersByOperationAndPreservesDisplayOrder() {
        Migration notApplied = migration("m240101_000001_not_applied", MigrationStatus.NotApply);
        Migration applied = migration("m240101_000002_applied", MigrationStatus.Success);
        Migration applyError = migration("m240101_000003_apply_error", MigrationStatus.ApplyError);
        Migration rollbackError = migration("m240101_000004_rollback_error", MigrationStatus.RollbackError);
        List<Migration> displayed = Arrays.asList(notApplied, applied, applyError, rollbackError);

        assertEquals(
                Arrays.asList(notApplied, applyError),
                MigrationSelectionPlanner.planAll(MigrationOperation.UP, displayed)
        );
        assertEquals(
                Arrays.asList(applied, rollbackError),
                MigrationSelectionPlanner.planAll(MigrationOperation.DOWN, displayed)
        );
        assertEquals(
                Arrays.asList(applied, rollbackError),
                MigrationSelectionPlanner.planAll(MigrationOperation.REDO, displayed)
        );
    }

    public void testUpPlansOldestFirstRegardlessOfDisplayOrder() {
        Migration oldest = migration("m240101_000001_oldest", MigrationStatus.NotApply);
        Migration selected = migration("m240101_000002_selected", MigrationStatus.NotApply);
        Migration newest = migration("m240101_000003_newest", MigrationStatus.NotApply);

        assertEquals(
                Arrays.asList(oldest, selected),
                MigrationSelectionPlanner.planToSelection(
                        MigrationOperation.UP,
                        Arrays.asList(oldest, selected, newest),
                        selected,
                        false
                )
        );
        assertEquals(
                Arrays.asList(oldest, selected),
                MigrationSelectionPlanner.planToSelection(
                        MigrationOperation.UP,
                        Arrays.asList(newest, selected, oldest),
                        selected,
                        true
                )
        );
    }

    public void testDownAndRedoPlanNewestFirstRegardlessOfDisplayOrder() {
        Migration oldest = migration("m240101_000001_oldest", MigrationStatus.Success);
        Migration selected = migration("m240101_000002_selected", MigrationStatus.Success);
        Migration newest = migration("m240101_000003_newest", MigrationStatus.Success);

        for (MigrationOperation operation : Arrays.asList(MigrationOperation.DOWN, MigrationOperation.REDO)) {
            assertEquals(
                    Arrays.asList(newest, selected),
                    MigrationSelectionPlanner.planToSelection(
                            operation,
                            Arrays.asList(oldest, selected, newest),
                            selected,
                            false
                    )
            );
            assertEquals(
                    Arrays.asList(newest, selected),
                    MigrationSelectionPlanner.planToSelection(
                            operation,
                            Arrays.asList(newest, selected, oldest),
                            selected,
                            true
                    )
            );
        }
    }

    public void testPlanToSelectionSkipsInapplicableMigrations() {
        Migration newest = migration("m240101_000003_newest", MigrationStatus.Success);
        Migration skipped = migration("m240101_000002_skipped", MigrationStatus.NotApply);
        Migration selected = migration("m240101_000001_selected", MigrationStatus.RollbackError);

        assertEquals(
                Arrays.asList(newest, selected),
                MigrationSelectionPlanner.planToSelection(
                        MigrationOperation.DOWN,
                        Arrays.asList(newest, skipped, selected),
                        selected,
                        true
                )
        );
    }

    public void testPlanToSelectionRejectsInapplicableOrMissingSelection() {
        Migration applied = migration("m240101_000001_applied", MigrationStatus.Success);
        Migration missing = migration("m240101_000002_missing", MigrationStatus.NotApply);

        assertEquals(
                Collections.emptyList(),
                MigrationSelectionPlanner.planToSelection(
                        MigrationOperation.UP,
                        Collections.singletonList(applied),
                        applied,
                        false
                )
        );
        assertEquals(
                Collections.emptyList(),
                MigrationSelectionPlanner.planToSelection(
                        MigrationOperation.UP,
                        Collections.singletonList(applied),
                        missing,
                        false
                )
        );
    }

    private static Migration migration(String name, MigrationStatus status) {
        PhpClass phpClass = (PhpClass) Proxy.newProxyInstance(
                PhpClass.class.getClassLoader(),
                new Class<?>[]{PhpClass.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getName")) {
                        return name;
                    }
                    if (method.getName().equals("getNamespaceName")) {
                        return "\\";
                    }
                    if (method.getName().equals("toString")) {
                        return name;
                    }
                    if (method.getName().equals("hashCode")) {
                        return System.identityHashCode(proxy);
                    }
                    if (method.getName().equals("equals")) {
                        return proxy == args[0];
                    }

                    return defaultValue(method.getReturnType());
                }
        );
        Migration migration = new Migration(phpClass, "migrations");
        migration.status = status;
        return migration;
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
