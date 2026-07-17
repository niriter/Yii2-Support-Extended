package com.nvlad.yii2support.migrations.commands;

import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.project.Project;
import com.nvlad.yii2support.migrations.ui.toolWindow.MigrationPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JTree;
import java.util.Objects;

/**
 * Immutable dependencies shared by migration commands created for one UI action.
 */
public record CommandContext(
        @NotNull Project project,
        @NotNull Application application,
        @NotNull MigrationPanel migrationPanel,
        @Nullable ConsoleView consoleView) {

    public CommandContext {
        Objects.requireNonNull(project, "project");
        Objects.requireNonNull(application, "application");
        Objects.requireNonNull(migrationPanel, "migrationPanel");
    }

    @NotNull
    public JTree migrationTree() {
        return migrationPanel.getTree();
    }
}
