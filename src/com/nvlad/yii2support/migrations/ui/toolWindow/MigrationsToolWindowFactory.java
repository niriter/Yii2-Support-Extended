package com.nvlad.yii2support.migrations.ui.toolWindow;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

public final class MigrationsToolWindowFactory implements ToolWindowFactory {
    public static final String TOOL_WINDOW_ID = "Migrations";

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        if (toolWindow.getContentManager().getContentCount() > 0) {
            return;
        }

        MigrationPanel migrationPanel = new MigrationPanel(project);
        ConsolePanel consolePanel = new ConsolePanel(project);
        ContentFactory contentFactory = ContentFactory.getInstance();

        Content navigator = contentFactory.createContent(migrationPanel, "Explorer", false);
        navigator.setDisposer(new MigrationsToolWindowController(project, toolWindow, migrationPanel.getTree()));
        toolWindow.getContentManager().addContent(navigator);

        Content console = contentFactory.createContent(consolePanel, "Output", false);
        console.setDisposer(consolePanel);
        toolWindow.getContentManager().addContent(console);
    }
}
