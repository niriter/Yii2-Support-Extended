package com.nvlad.yii2support.migrations.ui.toolWindow;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.CheckboxTree;
import com.intellij.ui.CheckboxTreeBase;
import com.intellij.ui.CheckedTreeNode;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.nvlad.yii2support.migrations.actions.*;
import com.nvlad.yii2support.migrations.services.MigrationService;
import com.nvlad.yii2support.migrations.util.TreeUtil;
import com.nvlad.yii2support.utils.Yii2SupportSettings;

import javax.swing.*;
import javax.swing.tree.TreeSelectionModel;

public class MigrationPanel extends SimpleToolWindowPanel {
    private final Project myProject;
    private CheckboxTree myTree;

    public MigrationPanel(Project project) {
        super(false);

        myProject = project;

        initContent();
        initToolBar();
    }

    public JTree getTree() {
        return myTree;
    }

    public void updateTree() {
        boolean newestFirst = Yii2SupportSettings.getInstance(myProject).newestFirst;
        MigrationService service = MigrationService.getInstance(myProject);
        TreeUtil.updateTree(myTree, service.getMigrationCommandMap(), newestFirst);
    }

    private void initContent() {
        MigrationTreeCellRenderer renderer = new MigrationTreeCellRenderer();
        CheckedTreeNode myRootNode = new CheckedTreeNode();
        myTree = new CheckboxTree(
                renderer,
                myRootNode,
                new CheckboxTreeBase.CheckPolicy(true, true, false, true)
        );
        myTree.addMouseListener(new MigrationsMouseListener());
        myTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

        JBScrollPane scrollPane = new JBScrollPane(myTree);
        scrollPane.setBorder(JBUI.Borders.empty());
        setContent(scrollPane);
    }


    private void initToolBar() {
        ActionToolbar toolbar = createToolbar();
        setToolbar(toolbar.getComponent());
    }

    private ActionToolbar createToolbar() {
        DefaultActionGroup group = new DefaultActionGroup();
        group.add(configureAction(new RefreshAction()));
        group.add(new Separator());
        group.add(configureAction(new MigrateUpAction()));
        group.add(configureAction(new MigrateDownAction()));
        group.add(configureAction(new MigrateRedoAction()));
        group.add(new Separator());
        group.add(configureAction(new OrderAscAction()));

        return ActionManager.getInstance().createActionToolbar("Migrations", group, false);
    }

    private <T extends AnActionButton> T configureAction(T action) {
        action.setContextComponent(this);
        return action;
    }
}
