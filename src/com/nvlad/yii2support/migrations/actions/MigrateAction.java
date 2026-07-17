package com.nvlad.yii2support.migrations.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.nvlad.yii2support.migrations.commands.CommandContext;
import com.nvlad.yii2support.migrations.commands.MigrationCommand;
import com.nvlad.yii2support.migrations.entities.DefaultMigrateCommand;
import com.nvlad.yii2support.migrations.entities.MigrateCommand;
import com.nvlad.yii2support.migrations.entities.Migration;
import com.nvlad.yii2support.migrations.entities.MigrationOperation;
import com.nvlad.yii2support.migrations.planning.MigrationSelectionPlanner;
import com.nvlad.yii2support.utils.Yii2SupportSettings;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

@SuppressWarnings("ComponentNotRegistered")
public final class MigrateAction extends MigrateBaseAction {
    private final MigrationOperation operation;

    public MigrateAction(@NotNull MigrationOperation operation, @NotNull String name, @NotNull Icon icon) {
        super(name, icon);
        this.operation = operation;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        DefaultMutableTreeNode treeNode = getSelectedNode();
        Project project = event.getProject();
        if (treeNode == null || project == null) {
            return;
        }

        Object userObject = treeNode.getUserObject();
        List<Migration> migrations;
        DefaultMutableTreeNode pathNode;

        if (userObject instanceof Migration) {
            Migration selectedMigration = (Migration) userObject;
            pathNode = (DefaultMutableTreeNode) treeNode.getParent();
            migrations = MigrationSelectionPlanner.planToSelection(
                    operation,
                    getChildMigrations(pathNode),
                    selectedMigration,
                    Yii2SupportSettings.getInstance(project).newestFirst
            );
        } else if ((userObject instanceof String || userObject instanceof MigrateCommand)
                && !(userObject instanceof DefaultMigrateCommand)) {
            pathNode = treeNode;
            migrations = MigrationSelectionPlanner.planAll(operation, getChildMigrations(treeNode));
        } else {
            return;
        }

        if (migrations.isEmpty()) {
            return;
        }

        CommandContext context = createCommandContext(project);
        MigrationCommand command = new MigrationCommand(
                operation,
                context,
                migrations,
                getCommand(treeNode),
                getMigrationPath(project, pathNode)
        );
        executeCommand(context, command);
    }

    @Override
    public boolean isEnabled() {
        DefaultMutableTreeNode treeNode = getSelectedNode();
        if (treeNode == null || !getTree().isEnabled()) {
            return false;
        }

        Object userObject = treeNode.getUserObject();
        if (userObject instanceof DefaultMigrateCommand) {
            return false;
        }

        if (userObject instanceof Migration) {
            return operation.accepts(((Migration) userObject).status);
        }

        return (userObject instanceof String || userObject instanceof MigrateCommand)
                && MigrationSelectionPlanner.hasApplicableMigration(operation, getChildMigrations(treeNode));
    }

    @NotNull
    private static List<Migration> getChildMigrations(@NotNull DefaultMutableTreeNode node) {
        List<Migration> migrations = new ArrayList<>();
        Enumeration<?> children = node.children();
        while (children.hasMoreElements()) {
            Object userObject = ((DefaultMutableTreeNode) children.nextElement()).getUserObject();
            if (userObject instanceof Migration) {
                migrations.add((Migration) userObject);
            }
        }

        return migrations;
    }
}
