package com.nvlad.yii2support.migrations.commands;

import com.intellij.database.dataSource.LocalDataSource;
import com.intellij.database.model.RawDataSource;
import com.intellij.database.psi.DbDataSource;
import com.intellij.database.psi.DbPsiFacade;
import com.intellij.database.util.DbImplUtil;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.openapi.util.text.StringUtil;
import com.nvlad.yii2support.common.YiiCommandLineUtil;
import com.nvlad.yii2support.migrations.compat.DataSourceSyncCompat;
import com.nvlad.yii2support.migrations.entities.DefaultMigrateCommand;
import com.nvlad.yii2support.migrations.entities.MigrateCommand;
import com.nvlad.yii2support.migrations.entities.Migration;
import com.nvlad.yii2support.migrations.entities.MigrationOperation;
import com.nvlad.yii2support.migrations.entities.MigrationStatus;
import org.jetbrains.annotations.NotNull;

import javax.swing.tree.DefaultMutableTreeNode;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MigrationCommand extends CommandBase {
    private static final Pattern MIGRATE_PATTERN = Pattern.compile("\\*\\*\\* (applying|applied|reverting|reverted|failed to apply|failed to revert) ([\\w\\\\-]*?\\\\)?([mM]\\d{6}_?\\d{6}\\D.+?)\\s+(\\(time: ([\\d.]+)s\\))?");

    private final MigrationOperation operation;
    private final String path;
    private final List<Migration> migrations;
    private String direction;
    private Map<String, DefaultMutableTreeNode> migrationNodeMap;
    private Map<Migration, MigrationStatus> migrationStatusMap;

    public MigrationCommand(
            @NotNull MigrationOperation operation,
            @NotNull CommandContext context,
            @NotNull List<Migration> migrations,
            @NotNull MigrateCommand command,
            String path) {
        super(context, command);
        this.operation = operation;
        this.path = path;
        this.migrations = migrations;
        direction = operation.getInitialDirection();
    }

    @Override
    public void run() {
        LinkedList<String> params = new LinkedList<>();
        params.add(String.valueOf(migrations.size()));
        prepareCommandParams(params);
        executeActionWithParams(params);
    }

    @Override
    void processOutput(String text) {
        Matcher matcher = MIGRATE_PATTERN.matcher(text);

        if (matcher.find()) {
            Migration migration = findMigration("\\" + StringUtil.defaultIfEmpty(matcher.group(2), ""), matcher.group(3));
            if (migration == null) {
                return;
            }

            switch (matcher.group(1)) {
                case "applying":
                    migration.status = MigrationStatus.Progress;
                    migration.applyAt = null;
                    migration.upDuration = null;

                    direction = "applying";
                    break;
                case "applied":
                    migration.status = MigrationStatus.Success;
                    migration.upDuration = Duration.parse("PT" + matcher.group(5) + "S");
                    break;
                case "failed to apply":
                    migration.status = MigrationStatus.ApplyError;
                    break;
                case "reverting":
                    migration.status = MigrationStatus.Progress;
                    migration.applyAt = null;
                    migration.upDuration = null;
                    migration.downDuration = null;

                    direction = "reverting";
                    break;
                case "reverted":
                    migration.status = MigrationStatus.NotApply;
                    migration.downDuration = Duration.parse("PT" + matcher.group(5) + "S");
                    break;
                case "failed to revert":
                    migration.status = MigrationStatus.RollbackError;
                    break;
            }

            repaintMigrationNode(migration);
        }
    }

    @Override
    DefaultMutableTreeNode findTreeNode(Migration migration) {
        return getMigrationNodeMap().get(migration.name);
    }

    private void executeActionWithParams(List<String> parameters) {
        try {
            String command = myCommand.command + "/" + operation.getCommandAction();
            ProcessHandler processHandler = YiiCommandLineUtil.configureHandler(myContext.project(), command, parameters);
            if (processHandler == null) {
                return;
            }

            migrationStatusMap = new HashMap<>();
            for (Migration migration : migrations) {
                migrationStatusMap.put(migration, migration.status);
            }

            executeProcess(processHandler);
            setErrorStatusForMigrationInProgress();
        } catch (ExecutionException e) {
            YiiCommandLineUtil.processError(e);
        }

        syncDataSources();
    }

    private Map<String, DefaultMutableTreeNode> getMigrationNodeMap() {
        if (migrationNodeMap == null) {
            migrationNodeMap = new HashMap<>();
            buildMigrationNodeMap((DefaultMutableTreeNode) myContext.migrationTree().getModel().getRoot());
        }

        return migrationNodeMap;
    }

    private void buildMigrationNodeMap(DefaultMutableTreeNode parentNode) {
        Enumeration<?> pathEnumeration = parentNode.children();
        while (pathEnumeration.hasMoreElements()) {
            DefaultMutableTreeNode node = ((DefaultMutableTreeNode) pathEnumeration.nextElement());
            if (node.getUserObject() instanceof Migration) {
                migrationNodeMap.put(((Migration) node.getUserObject()).name, node);
            }
        }
    }

    private void setErrorStatusForMigrationInProgress() {
        for (Migration migration : migrations) {
            if (isInvalidMigrationStatus(migration)) {
                migration.status = "reverting".equals(direction)
                        ? MigrationStatus.RollbackError
                        : MigrationStatus.ApplyError;
                repaintMigrationNode(migration);
            }
        }
    }

    private boolean isInvalidMigrationStatus(Migration migration) {
        if (migration.status == MigrationStatus.Progress) {
            return true;
        }

        if (operation == MigrationOperation.REDO && migration.status != migrationStatusMap.get(migration)) {
            return true;
        }

        return operation != MigrationOperation.REDO && migration.status == migrationStatusMap.get(migration);
    }

    private void syncDataSources() {
        DbPsiFacade facade = DbPsiFacade.getInstance(myContext.project());
        for (DbDataSource dataSource : facade.getDataSources()) {
            RawDataSource rawDataSource = dataSource.getDelegateDataSource();
            if (rawDataSource instanceof LocalDataSource
                    && DbImplUtil.isConnected(myContext.project(), (LocalDataSource) rawDataSource)) {
                DataSourceSyncCompat.synchronize(myContext.project(), (LocalDataSource) rawDataSource);
            }
        }
    }

    private Migration findMigration(String namespace, String name) {
        for (Migration migration : migrations) {
            if (StringUtil.equals(name, migration.name) && StringUtil.equals(namespace, migration.namespace)) {
                return migration;
            }
        }

        return null;
    }

    private void prepareCommandParams(List<String> params) {
        boolean useNamespaces = migrations.stream().anyMatch(migration -> !migration.namespace.equals("\\"));
        super.prepareCommandParams(params, useNamespaces ? "@vendor" : path);
        if (myCommand instanceof DefaultMigrateCommand && useNamespaces) {
            Set<String> namespaces = new HashSet<>();
            for (Migration migration : migrations) {
                if (!migration.namespace.equals("\\")) {
                    namespaces.add(migration.namespace);
                }
            }

            params.add("--migrationNamespaces=" + StringUtil.join(namespaces, ","));
        }
    }
}
