package com.nvlad.yii2support.migrations.entities;

import org.jetbrains.annotations.NotNull;

public enum MigrationOperation {
    UP("up", "applying", false, false),
    DOWN("down", "reverting", true, true),
    REDO("redo", "reverting", true, true);

    private final String commandAction;
    private final String initialDirection;
    private final boolean runsNewestFirst;
    private final boolean operatesOnAppliedMigrations;

    MigrationOperation(
            String commandAction,
            String initialDirection,
            boolean runsNewestFirst,
            boolean operatesOnAppliedMigrations) {
        this.commandAction = commandAction;
        this.initialDirection = initialDirection;
        this.runsNewestFirst = runsNewestFirst;
        this.operatesOnAppliedMigrations = operatesOnAppliedMigrations;
    }

    @NotNull
    public String getCommandAction() {
        return commandAction;
    }

    @NotNull
    public String getInitialDirection() {
        return initialDirection;
    }

    public boolean runsNewestFirst() {
        return runsNewestFirst;
    }

    public boolean accepts(@NotNull MigrationStatus status) {
        boolean isApplied = status == MigrationStatus.Success || status == MigrationStatus.RollbackError;
        return operatesOnAppliedMigrations == isApplied;
    }
}
