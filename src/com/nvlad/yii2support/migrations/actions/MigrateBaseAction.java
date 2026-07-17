package com.nvlad.yii2support.migrations.actions;

import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.content.Content;
import com.nvlad.yii2support.common.YiiApplicationUtils;
import com.nvlad.yii2support.migrations.commands.CommandBase;
import com.nvlad.yii2support.migrations.commands.CommandContext;
import com.nvlad.yii2support.migrations.entities.MigrateCommand;
import com.nvlad.yii2support.migrations.ui.toolWindow.ConsolePanel;
import com.nvlad.yii2support.migrations.ui.toolWindow.MigrationPanel;
import com.nvlad.yii2support.migrations.ui.toolWindow.MigrationsToolWindowFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

abstract class MigrateBaseAction extends AnActionButton {
    MigrateBaseAction(String name, Icon icon) {
        super(name, icon);
    }

    @NotNull
    MigrationPanel getPanel() {
        return (MigrationPanel) getContextComponent();
    }

    @NotNull
    JTree getTree() {
        return getPanel().getTree();
    }

    @Nullable
    DefaultMutableTreeNode getSelectedNode() {
        JTree tree = getTree();

        if (tree.getSelectionModel().getSelectionCount() > 0) {
            TreePath leadSelectionPath = tree.getLeadSelectionPath();
            if (leadSelectionPath == null) {
                return null;
            }

            return (DefaultMutableTreeNode) leadSelectionPath.getLastPathComponent();
        }

        return null;
    }

    void executeCommand(CommandContext context, CommandBase ...command) {
        executeCommand(context, Arrays.asList(command));
    }

    void executeCommand(CommandContext context, List<CommandBase> commands) {
        context.application().executeOnPooledThread(() -> {
            for (CommandBase command : commands) {
                command.run();
            }
        });
    }

    @NotNull
    CommandContext createCommandContext(@NotNull Project project) {
        ConsoleView consoleView = null;
        ToolWindow window = ToolWindowManager
                .getInstance(project).getToolWindow(MigrationsToolWindowFactory.TOOL_WINDOW_ID);

        if (window != null) {
            for (Content content : window.getContentManager().getContents()) {
                JComponent component = content.getComponent();
                if (component instanceof ConsolePanel consolePanel) {
                    consoleView = consolePanel.getConsoleView();
                    break;
                }
            }
        }

        Application application = ApplicationManager.getApplication();
        return new CommandContext(
                project,
                application,
                getPanel(),
                consoleView
        );
    }

    @NotNull
    MigrateCommand getCommand(@NotNull DefaultMutableTreeNode node) {
        if (node.getUserObject() instanceof MigrateCommand command) {
            return command;
        }

        return getCommand((DefaultMutableTreeNode) node.getParent());
    }

    @Nullable
    String getMigrationPath(Project project, TreeNode node) {
        String projectRoot = YiiApplicationUtils.getYiiRootPath(project) + "/";
        Object userObject = ((DefaultMutableTreeNode) node).getUserObject();
        if (userObject instanceof MigrateCommand command) {
            List<String> paths = new ArrayList<>();
            for (String s : command.migrationPath) {
                String preparePath = preparePath(s, projectRoot);
                paths.add(preparePath);
            }

            return StringUtil.join(paths, ",");
        }

        if (userObject instanceof String path) {
            return preparePath(path, projectRoot);
        }

        return null;
    }

    private String preparePath(String path, String projectRoot) {
        if (path.startsWith("@") || path.startsWith("/")) {
            return path;
        }

        if (path.charAt(1) == ':') {
            return path;
        }

        return projectRoot + path;
    }
}
