package com.nvlad.yii2support;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class PluginStartupActivity implements ProjectActivity {
    @Nullable
    @Override
    public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        IdeaPluginDescriptor plugin = PluginManagerCore.getPlugin(PluginMetadata.PLUGIN_ID);
        if (plugin == null || !PluginGlobalSettings.getInstance().markVersionNotified(plugin.getVersion())) {
            return Unit.INSTANCE;
        }

        NotificationGroupManager.getInstance()
                .getNotificationGroup(PluginMetadata.NOTIFICATION_GROUP_ID)
                .createNotification(
                        plugin.getName() + " v" + plugin.getVersion(),
                        plugin.getChangeNotes(),
                        NotificationType.INFORMATION
                )
                .notify(project);

        return Unit.INSTANCE;
    }
}
