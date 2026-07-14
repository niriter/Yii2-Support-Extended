package com.nvlad.yii2support.migrations.ui.toolWindow;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ex.ToolWindowManagerListener;
import com.nvlad.yii2support.migrations.services.MigrationService;
import com.nvlad.yii2support.migrations.services.MigrationsVirtualFileMonitor;
import com.nvlad.yii2support.migrations.util.TreeUtil;
import com.nvlad.yii2support.utils.Yii2SupportSettings;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

final class MigrationsToolWindowController implements Disposable {
    private final Project project;
    private final ToolWindow toolWindow;
    private final JTree tree;
    private final MigrationService migrationService;
    private final VisibilityLifecycle visibilityLifecycle;
    private volatile boolean disposed;

    MigrationsToolWindowController(Project project, ToolWindow toolWindow, JTree tree) {
        this.project = project;
        this.toolWindow = toolWindow;
        this.tree = tree;
        migrationService = MigrationService.getInstance(project);
        visibilityLifecycle = new VisibilityLifecycle(this::subscribeVisibleListeners);

        project.getMessageBus()
                .connect(this)
                .subscribe(ToolWindowManagerListener.TOPIC, new ToolWindowManagerListener() {
                    @Override
                    public void stateChanged(@NotNull ToolWindowManager toolWindowManager) {
                        updateVisibility();
                    }
                });

        updateVisibility();
    }

    @Override
    public void dispose() {
        disposed = true;
        visibilityLifecycle.close();
    }

    private void updateVisibility() {
        visibilityLifecycle.setVisible(toolWindow.isVisible());
    }

    private VisibilityLifecycle.Subscription subscribeVisibleListeners() {
        Disposable visibleSession = Disposer.newDisposable("Yii2 migrations visible session");
        migrationService.addListener(this::queueTreeUpdate, visibleSession);
        VirtualFileManager.getInstance().addVirtualFileListener(
                new MigrationsVirtualFileMonitor(project),
                visibleSession
        );

        queueTreeUpdate();
        migrationService.syncAsync();
        return () -> Disposer.dispose(visibleSession);
    }

    private void queueTreeUpdate() {
        Runnable update = () -> {
            if (disposed || project.isDisposed() || !visibilityLifecycle.isVisible()) {
                return;
            }

            TreeUtil.updateTree(
                    tree,
                    migrationService.getMigrationCommandMap(),
                    Yii2SupportSettings.getInstance(project).newestFirst
            );
        };

        if (ApplicationManager.getApplication().isDispatchThread()) {
            update.run();
        } else {
            ApplicationManager.getApplication().invokeLater(update);
        }
    }
}
