package com.nvlad.yii2support.migrations.services;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.nvlad.yii2support.common.FileUtil;
import com.nvlad.yii2support.common.YiiApplicationUtils;
import com.nvlad.yii2support.migrations.entities.DefaultMigrateCommand;
import com.nvlad.yii2support.migrations.entities.MigrateCommand;
import com.nvlad.yii2support.migrations.entities.MigrateCommandComparator;
import com.nvlad.yii2support.migrations.entities.Migration;
import com.nvlad.yii2support.utils.Yii2SupportSettings;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;

@Service(Service.Level.PROJECT)
public final class MigrationService implements Disposable {
    private final Project project;
    private final Set<MigrationServiceListener> listeners = new CopyOnWriteArraySet<>();
    private final AtomicBoolean syncRequested = new AtomicBoolean();
    private final AtomicBoolean syncWorkerRunning = new AtomicBoolean();

    private volatile Map<MigrateCommand, Collection<Migration>> migrationMap = Collections.emptyMap();
    private volatile List<Migration> migrations = Collections.emptyList();
    private volatile boolean initialized;
    private volatile boolean disposed;

    public MigrationService(Project project) {
        this.project = project;
    }

    public static MigrationService getInstance(Project project) {
        return project.getService(MigrationService.class);
    }

    @NotNull
    public Map<MigrateCommand, Collection<Migration>> getMigrationCommandMap() {
        return migrationMap;
    }

    @NotNull
    public List<Migration> getMigrations() {
        return migrations;
    }

    public synchronized void sync() {
        if (disposed || project.isDisposed()) {
            return;
        }

        MigrationSnapshot snapshot = ApplicationManager.getApplication().runReadAction(
                (Computable<MigrationSnapshot>) this::buildSnapshot
        );
        if (snapshot == null || disposed) {
            return;
        }

        boolean changed = !initialized || !snapshot.migrationMap().equals(migrationMap);
        migrationMap = snapshot.migrationMap();
        migrations = snapshot.migrations();
        initialized = true;

        if (changed) {
            listeners.forEach(MigrationServiceListener::treeChanged);
        }
    }

    public void syncAsync() {
        if (disposed || project.isDisposed()) {
            return;
        }

        syncRequested.set(true);
        if (!syncWorkerRunning.compareAndSet(false, true)) {
            return;
        }

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                while (syncRequested.getAndSet(false) && !disposed && !project.isDisposed()) {
                    sync();
                }
            } finally {
                syncWorkerRunning.set(false);
                if (syncRequested.get() && !disposed) {
                    syncAsync();
                }
            }
        });
    }

    public void addListener(@NotNull MigrationServiceListener listener, @NotNull Disposable parentDisposable) {
        listeners.add(listener);
        Disposer.register(parentDisposable, () -> listeners.remove(listener));
    }

    int getListenerCountForTests() {
        return listeners.size();
    }

    @Override
    public void dispose() {
        disposed = true;
        listeners.clear();
        migrationMap = Collections.emptyMap();
        migrations = Collections.emptyList();
    }

    private MigrationSnapshot buildSnapshot() {
        Collection<PhpClass> migrationClasses = new ArrayList<>();
        try {
            PhpIndex.getInstance(project).processAllSubclasses(
                    "\\yii\\db\\MigrationInterface",
                    migrationClass -> migrationClasses.add(migrationClass)
            );
        } catch (IndexNotReadyException exception) {
            return null;
        }

        String projectRootUrl = YiiApplicationUtils.getYiiRootUrl(project);
        if (projectRootUrl == null) {
            return new MigrationSnapshot(Collections.emptyMap(), Collections.emptyList());
        }

        List<MigrateCommand> commands = new ArrayList<>(Yii2SupportSettings.getInstance(project).migrateCommands);
        commands.add(new DefaultMigrateCommand(commands));
        commands.sort(new MigrateCommandComparator());

        Map<MigrateCommand, Collection<Migration>> newMigrationMap = new LinkedHashMap<>();
        List<Migration> newMigrations = new ArrayList<>();
        for (MigrateCommand command : commands) {
            newMigrationMap.put(command, new ArrayList<>());
        }

        for (PhpClass migrationClass : migrationClasses) {
            if (migrationClass.isAbstract() || !Migration.isValidMigrationClass(migrationClass)) {
                continue;
            }

            VirtualFile virtualFile = FileUtil.getVirtualFile(migrationClass.getContainingFile());
            if (virtualFile == null || !virtualFile.getUrl().startsWith(projectRootUrl + "/")) {
                continue;
            }

            String path = virtualFile.getUrl().substring(projectRootUrl.length() + 1);
            path = path.substring(0, path.length() - virtualFile.getName().length() - 1);
            Migration migration = getMigrationForClass(migrationClass, path);
            for (MigrateCommand command : commands) {
                if (command.containsMigration(project, migration)) {
                    newMigrationMap.get(command).add(migration);
                    newMigrations.add(migration);
                    break;
                }
            }
        }

        Map<MigrateCommand, Collection<Migration>> immutableMap = new LinkedHashMap<>();
        for (Map.Entry<MigrateCommand, Collection<Migration>> entry : newMigrationMap.entrySet()) {
            immutableMap.put(entry.getKey(), Collections.unmodifiableList(new ArrayList<>(entry.getValue())));
        }

        return new MigrationSnapshot(
                Collections.unmodifiableMap(immutableMap),
                Collections.unmodifiableList(new ArrayList<>(newMigrations))
        );
    }

    private Migration getMigrationForClass(PhpClass phpClass, String path) {
        for (Migration migration : migrations) {
            if (migration.migrationClass.equals(phpClass)) {
                return migration;
            }
        }
        return new Migration(phpClass, path);
    }

    private record MigrationSnapshot(
            Map<MigrateCommand, Collection<Migration>> migrationMap,
            List<Migration> migrations
    ) {
    }
}
