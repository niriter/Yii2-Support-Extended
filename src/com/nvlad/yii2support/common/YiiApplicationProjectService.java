package com.nvlad.yii2support.common;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Service(Service.Level.PROJECT)
public final class YiiApplicationProjectService {
    private final Project project;
    private volatile boolean rootResolved;
    private volatile VirtualFile yiiRoot;

    public YiiApplicationProjectService(Project project) {
        this.project = project;
    }

    @Nullable
    public VirtualFile getYiiRootVirtualFile(@Nullable String path) {
        if (rootResolved) {
            return yiiRoot;
        }

        synchronized (this) {
            if (!rootResolved) {
                yiiRoot = resolveYiiRoot(path);
                rootResolved = true;
            }
            return yiiRoot;
        }
    }

    public synchronized void resetYiiRootPath() {
        yiiRoot = null;
        rootResolved = false;
    }

    @Nullable
    private VirtualFile resolveYiiRoot(@Nullable String path) {
        VirtualFile projectRoot = ProjectUtil.guessProjectDir(project);
        if (path == null) {
            return projectRoot;
        }

        VirtualFile root = LocalFileSystem.getInstance().refreshAndFindFileByPath(path);
        if (root != null || projectRoot == null) {
            return root;
        }

        path = path.replace('\\', '/');
        if (path.startsWith("./")) {
            path = path.substring(2);
        }
        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        root = projectRoot;
        List<String> pathEntries = StringUtil.split(path, "/");
        for (String pathEntry : pathEntries) {
            root = root.findChild(pathEntry);
            if (root == null) {
                break;
            }
        }
        return root;
    }
}
