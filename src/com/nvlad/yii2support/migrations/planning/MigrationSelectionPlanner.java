package com.nvlad.yii2support.migrations.planning;

import com.nvlad.yii2support.migrations.entities.Migration;
import com.nvlad.yii2support.migrations.entities.MigrationOperation;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MigrationSelectionPlanner {
    private MigrationSelectionPlanner() {
    }

    @NotNull
    public static List<Migration> planAll(
            @NotNull MigrationOperation operation,
            @NotNull List<Migration> migrations) {
        List<Migration> result = new ArrayList<>();
        for (Migration migration : migrations) {
            if (operation.accepts(migration.status)) {
                result.add(migration);
            }
        }

        return result;
    }

    @NotNull
    public static List<Migration> planToSelection(
            @NotNull MigrationOperation operation,
            @NotNull List<Migration> displayedMigrations,
            @NotNull Migration selectedMigration,
            boolean displayedNewestFirst) {
        if (!operation.accepts(selectedMigration.status)) {
            return Collections.emptyList();
        }

        List<Migration> executionOrder = new ArrayList<>(displayedMigrations);
        if (operation.runsNewestFirst() != displayedNewestFirst) {
            Collections.reverse(executionOrder);
        }

        List<Migration> result = new ArrayList<>();
        for (Migration migration : executionOrder) {
            if (operation.accepts(migration.status)) {
                result.add(migration);
            }

            if (migration == selectedMigration) {
                return result;
            }
        }

        return Collections.emptyList();
    }

    public static boolean hasApplicableMigration(
            @NotNull MigrationOperation operation,
            @NotNull List<Migration> migrations) {
        return migrations.stream().anyMatch(migration -> operation.accepts(migration.status));
    }
}
