package com.nvlad.yii2support.migrations.commands;

import com.intellij.execution.process.AnsiEscapeDecoder;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.nvlad.yii2support.migrations.entities.MigrateCommand;
import com.nvlad.yii2support.migrations.entities.Migration;
import org.jetbrains.annotations.NotNull;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.nio.charset.StandardCharsets;
import java.util.List;

public abstract class CommandBase implements Runnable {
    final CommandContext myContext;
    final MigrateCommand myCommand;

    CommandBase(@NotNull CommandContext context, @NotNull MigrateCommand command) {
        myContext = context;
        myCommand = command;
    }

    abstract void processOutput(String text);

    void repaintMigrationNode(Migration migration) {
        DefaultMutableTreeNode treeNode = findTreeNode(migration);
        if (treeNode != null) {
            myContext.application().invokeLater(() ->
                    ((DefaultTreeModel) myContext.migrationTree().getModel()).nodeChanged(treeNode));
        }
    }

    abstract DefaultMutableTreeNode findTreeNode(Migration migration);

    Integer executeProcess(@NotNull ProcessHandler processHandler) {
        ConsoleView consoleView = myContext.consoleView();
        if (consoleView != null && consoleView.getContentSize() > 0) {
            consoleView.print("\n*************************************\n\n", ConsoleViewContentType.NORMAL_OUTPUT);
        }

        processHandler.addProcessListener(new CommandProcessListener(this));
        processHandler.startNotify();

        JTree migrationTree = myContext.migrationTree();
        myContext.application().invokeLater(() -> migrationTree.setEnabled(false));

        try {
            processHandler.waitFor();
            return processHandler.getExitCode();
        } finally {
            myContext.application().invokeLater(() -> migrationTree.setEnabled(true));
        }
    }

    void prepareCommandParams(List<String> params, String path) {
        if (!StringUtil.isEmpty(path)) {
            params.add("--migrationPath=" + path);
        }

        if (!StringUtil.isEmpty(myCommand.db)) {
            params.add("--db=" + myCommand.db);
        }

        if (myCommand.migrationTable != null) {
            params.add("--migrationTable=" + myCommand.migrationTable);
        }
//        params.add("--useTablePrefix=" + (myCommand.useTablePrefix ? "1" : "0")); // only for "create" action
        params.add("--interactive=0");
    }

    class CommandProcessListener implements ProcessListener {
        private final AnsiEscapeDecoder decoder = new AnsiEscapeDecoder();
        private final CommandBase myProcessor;

        CommandProcessListener(CommandBase processor) {
            myProcessor = processor;
        }

        @Override
        public void startNotified(@NotNull ProcessEvent processEvent) {

        }

        @Override
        public void processTerminated(@NotNull ProcessEvent processEvent) {

        }

        @Override
        public void processWillTerminate(@NotNull ProcessEvent processEvent, boolean b) {

        }

        @Override
        public void onTextAvailable(@NotNull ProcessEvent processEvent, @NotNull Key key) {
            final StringBuilder builder = new StringBuilder();
            final boolean toUtf8 = SystemInfo.isWindows && key.toString().equals("stdout");
            decoder.escapeText(processEvent.getText(), key, (text, processOutputType) -> {
                if (toUtf8) {
                    text = new String(text.getBytes(), StandardCharsets.UTF_8);
                }

                builder.append(text);

                ConsoleView consoleView = myContext.consoleView();
                if (consoleView != null) {
                    consoleView.print(text, ConsoleViewContentType.getConsoleViewType(processOutputType));
                }
            });

            myProcessor.processOutput(builder.toString());
        }
    }
}
