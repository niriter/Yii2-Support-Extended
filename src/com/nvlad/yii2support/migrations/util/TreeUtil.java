package com.nvlad.yii2support.migrations.util;

import com.intellij.ui.CheckedTreeNode;
import com.nvlad.yii2support.migrations.entities.DefaultMigrateCommand;
import com.nvlad.yii2support.migrations.entities.MigrateCommand;
import com.nvlad.yii2support.migrations.entities.MigrateCommandComparator;
import com.nvlad.yii2support.migrations.entities.Migration;
import com.nvlad.yii2support.migrations.entities.MigrationComparator;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Consumer;

import static com.intellij.util.ui.tree.TreeUtil.collectExpandedPaths;
import static com.intellij.util.ui.tree.TreeUtil.collectSelectedPaths;

public final class TreeUtil {
    private TreeUtil() {
    }

    public static void updateTree(
            JTree tree,
            Map<MigrateCommand, Collection<Migration>> migrationMap,
        boolean newestFirst
    ) {
        Object currentRoot = tree.getModel().getRoot();
        if (!(currentRoot instanceof DefaultMutableTreeNode oldRoot)) {
            return;
        }

        TreeState state = captureState(tree, oldRoot);
        CheckedTreeNode newRoot = buildTree(snapshot(migrationMap, newestFirst), oldRoot);

        tree.setModel(new DefaultTreeModel(newRoot));
        restoreState(tree, newRoot, state);
    }

    private static TreeSnapshot snapshot(
            Map<MigrateCommand, Collection<Migration>> migrationMap,
            boolean newestFirst
    ) {
        List<CommandSnapshot> commands = new ArrayList<>();
        for (Map.Entry<MigrateCommand, Collection<Migration>> entry : migrationMap.entrySet()) {
            Collection<Migration> commandMigrations = entry.getValue();
            if (commandMigrations == null || commandMigrations.isEmpty()) {
                continue;
            }

            List<Migration> migrations = new ArrayList<>(commandMigrations);
            migrations.sort(new MigrationComparator(newestFirst));
            commands.add(new CommandSnapshot(entry.getKey(), List.copyOf(migrations)));
        }

        MigrateCommandComparator comparator = new MigrateCommandComparator();
        commands.sort((left, right) -> comparator.compare(left.command(), right.command()));
        return new TreeSnapshot(List.copyOf(commands));
    }

    private static CheckedTreeNode buildTree(TreeSnapshot snapshot, DefaultMutableTreeNode oldRoot) {
        CheckedTreeNode root = new CheckedTreeNode(oldRoot.getUserObject());
        if (oldRoot instanceof CheckedTreeNode checkedRoot) {
            root.setChecked(checkedRoot.isChecked());
            root.setEnabled(checkedRoot.isEnabled());
        }

        for (CommandSnapshot command : snapshot.commands()) {
            DefaultMutableTreeNode commandNode = new DefaultMutableTreeNode(command.command());
            root.add(commandNode);

            if (command.command() instanceof DefaultMigrateCommand) {
                Map<String, List<Migration>> migrationsByPath = new TreeMap<>();
                for (Migration migration : command.migrations()) {
                    migrationsByPath.computeIfAbsent(migration.path, ignored -> new ArrayList<>()).add(migration);
                }
                for (Map.Entry<String, List<Migration>> entry : migrationsByPath.entrySet()) {
                    DefaultMutableTreeNode pathNode = new DefaultMutableTreeNode(entry.getKey());
                    commandNode.add(pathNode);
                    addMigrationNodes(pathNode, entry.getValue());
                }
            } else {
                addMigrationNodes(commandNode, command.migrations());
            }
        }

        return root;
    }

    private static void addMigrationNodes(DefaultMutableTreeNode parent, Collection<Migration> migrations) {
        for (Migration migration : migrations) {
            parent.add(new DefaultMutableTreeNode(migration));
        }
    }

    private static TreeState captureState(JTree tree, DefaultMutableTreeNode root) {
        return new TreeState(
                List.copyOf(collectExpandedPaths(tree, new TreePath(root))),
                List.copyOf(collectSelectedPaths(tree, new TreePath(root))),
                root.getChildCount() == 0 || tree.isExpanded(new TreePath(root))
        );
    }

    private static void restoreState(JTree tree, DefaultMutableTreeNode root, TreeState state) {
        TreePath rootPath = new TreePath(root);
        tree.collapsePath(rootPath);
        if (state.expandRoot()) {
            tree.expandPath(rootPath);
        }
        restorePaths(root, state.expanded(), tree::expandPath);

        List<TreePath> selection = new ArrayList<>();
        restorePaths(root, state.selected(), selection::add);
        tree.setSelectionPaths(selection.toArray(new TreePath[0]));
    }

    private static void restorePaths(
            DefaultMutableTreeNode root,
            List<TreePath> paths,
            Consumer<TreePath> consumer
    ) {
        for (TreePath path : paths) {
            TreePath restoredPath = findPath(root, path);
            if (restoredPath != null) {
                consumer.accept(restoredPath);
            }
        }
    }

    private static TreePath findPath(DefaultMutableTreeNode root, TreePath oldPath) {
        Object[] oldNodes = oldPath.getPath();
        Object[] newNodes = new Object[oldNodes.length];
        newNodes[0] = root;
        DefaultMutableTreeNode parent = root;

        for (int i = 1; i < oldNodes.length; i++) {
            Object key = nodeKey(oldNodes[i]);
            DefaultMutableTreeNode matchingChild = null;
            Enumeration<?> children = parent.children();
            while (children.hasMoreElements()) {
                DefaultMutableTreeNode child = (DefaultMutableTreeNode) children.nextElement();
                if (Objects.equals(key, nodeKey(child))) {
                    matchingChild = child;
                    break;
                }
            }
            if (matchingChild == null) {
                return null;
            }
            newNodes[i] = matchingChild;
            parent = matchingChild;
        }

        return new TreePath(newNodes);
    }

    private static Object nodeKey(Object component) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) component;
        Object userObject = node.getUserObject();
        if (userObject instanceof MigrateCommand command) {
            return new CommandKey(command instanceof DefaultMigrateCommand, command.isDefault, command.command);
        }
        if (userObject instanceof Migration migration) {
            return new MigrationKey(migration.namespace, migration.name, migration.path);
        }
        return userObject;
    }

    private record TreeSnapshot(List<CommandSnapshot> commands) {
    }

    private record CommandSnapshot(MigrateCommand command, List<Migration> migrations) {
    }

    private record TreeState(List<TreePath> expanded, List<TreePath> selected, boolean expandRoot) {
    }

    private record CommandKey(boolean syntheticDefault, boolean configuredDefault, String command) {
    }

    private record MigrationKey(String namespace, String name, String path) {
    }
}
