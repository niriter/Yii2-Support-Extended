package com.nvlad.yii2support.common;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.nvlad.yii2support.utils.Yii2SupportSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service(Service.Level.PROJECT)
public final class YiiAlias {
    public static YiiAlias getInstance(Project project) {
        return project.getService(YiiAlias.class);
    }

    private final Project myProject;
    private final ConcurrentMap<String, String> myResolvedAliasCache = new ConcurrentHashMap<>();
    private volatile Map<String, String> myAliasMap;

    public YiiAlias(Project project) {
        myProject = project;
    }

    @Nullable
    public String getAlias(@NotNull String alias, boolean console) {
        return aliasFromMap(getAliasMap(), alias, console);
    }

    @Nullable
    public String resolveAlias(@NotNull String alias, boolean console) {
        String cacheKey = (console ? "console:" : "web:") + alias;
        String path = myResolvedAliasCache.get(cacheKey);
        if (path != null) {
            return path;
        }

        path = getAlias(alias, console);
        if (path == null) {
            return null;
        }

        //myResolvedAliasCache.put(alias, StringUtils.stripStart(path, "/"));
        path = path.replaceFirst("^/+", "");
        myResolvedAliasCache.put(cacheKey, path);

        return path;
    }

    public VirtualFile resolveVirtualFile(@NotNull String alias, boolean console) {
        String path = resolveAlias(alias, console);
        if (path == null) {
            return null;
        }

        path = YiiApplicationUtils.getYiiRootPath(myProject) + "/" + path;

        return LocalFileSystem.getInstance().findFileByPath(path);
    }

    public synchronized void reset() {
        myAliasMap = null;
        myResolvedAliasCache.clear();
    }

    @NotNull
    private Map<String, String> getAliasMap() {
        Map<String, String> aliases = myAliasMap;
        if (aliases != null) {
            return aliases;
        }

        synchronized (this) {
            if (myAliasMap == null) {
                myAliasMap = Collections.unmodifiableMap(
                        new HashMap<>(Yii2SupportSettings.getInstance(myProject).aliasMap)
                );
            }
            return myAliasMap;
        }
    }

    @Nullable
    private String aliasFromMap(@NotNull Map<String, String> aliasMap, @NotNull String alias, boolean console) {
        if (console && alias.startsWith("@app/")) {
            alias = "@yii2support-console-command-app-root" + alias.substring(4);
        }

        Set<String> visited = new HashSet<>();
        while (alias.startsWith("@")) {
            if (!visited.add(alias)) {
                return null;
            }

            String value = aliasMap.get(alias);
            if (value == null) {
                String foundAlias = null;
                for (String aliasKey : aliasMap.keySet()) {
                    if (alias.startsWith(aliasKey)
                            && (foundAlias == null || aliasKey.length() > foundAlias.length())) {
                        foundAlias = aliasKey;
                    }
                }

                if (foundAlias == null) {
                    return null;
                }
                value = aliasMap.get(foundAlias) + alias.substring(foundAlias.length());
            }

            alias = value;
        }

        return alias;
    }
}
