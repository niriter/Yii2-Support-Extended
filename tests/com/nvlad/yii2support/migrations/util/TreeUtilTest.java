package com.nvlad.yii2support.migrations.util;

import com.intellij.ui.CheckedTreeNode;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.nvlad.yii2support.migrations.entities.DefaultMigrateCommand;
import com.nvlad.yii2support.migrations.entities.MigrateCommand;
import com.nvlad.yii2support.migrations.entities.Migration;
import junit.framework.TestCase;

import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TreeUtilTest extends TestCase {
    public void testRebuildsSortedPathTreeWithoutMutatingSnapshot() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            MigrateCommand configuredDefault = command("migrate", true);
            DefaultMigrateCommand defaultCommand = new DefaultMigrateCommand(List.of(configuredDefault));
            MigrateCommand emptyCommand = command("empty", false);

            Migration oldest = migration("m240101_000000_oldest", "migrations/a");
            Migration newest = migration("m240301_000000_newest", "migrations/a");
            Migration otherPath = migration("m240201_000000_other", "migrations/b");
            List<Migration> sourceOrder = new ArrayList<>(List.of(oldest, newest, otherPath));

            Map<MigrateCommand, Collection<Migration>> snapshot = new LinkedHashMap<>();
            snapshot.put(emptyCommand, List.of());
            snapshot.put(defaultCommand, sourceOrder);

            JTree tree = tree();
            TreeUtil.updateTree(tree, snapshot, true);

            DefaultMutableTreeNode root = root(tree);
            assertEquals(1, root.getChildCount());
            assertTrue(tree.isExpanded(new TreePath(root)));
            DefaultMutableTreeNode commandNode = child(root, 0);
            assertSame(defaultCommand, commandNode.getUserObject());
            assertEquals("migrations/a", child(commandNode, 0).getUserObject());
            assertEquals("migrations/b", child(commandNode, 1).getUserObject());
            assertSame(newest, child(child(commandNode, 0), 0).getUserObject());
            assertSame(oldest, child(child(commandNode, 0), 1).getUserObject());
            assertEquals(List.of(oldest, newest, otherPath), sourceOrder);
        });
    }

    public void testPreservesExpansionAndSelectionAcrossModelReplacement() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            MigrateCommand oldCommand = command("tenant-migrate", false);
            Migration oldSelected = migration("m240201_000000_selected", "tenant/migrations");
            JTree tree = tree();
            TreeUtil.updateTree(tree, Map.of(oldCommand, List.of(oldSelected)), false);

            DefaultTreeModel oldModel = (DefaultTreeModel) tree.getModel();
            DefaultMutableTreeNode oldRoot = root(tree);
            DefaultMutableTreeNode oldCommandNode = child(oldRoot, 0);
            TreePath commandPath = new TreePath(oldCommandNode.getPath());
            tree.expandPath(commandPath);
            tree.setSelectionPath(new TreePath(child(oldCommandNode, 0).getPath()));
            assertTrue(tree.isExpanded(commandPath));

            MigrateCommand newCommand = command("tenant-migrate", false);
            MigrateCommand newEarlierCommand = command("alpha-migrate", false);
            Migration newEarlier = migration("m240101_000000_earlier", "tenant/migrations");
            Migration newSelected = migration("m240201_000000_selected", "tenant/migrations");
            Migration otherCommandMigration = migration("m240101_000000_other", "other/migrations");
            TreeUtil.updateTree(
                    tree,
                    Map.of(
                            newCommand, List.of(newSelected, newEarlier),
                            newEarlierCommand, List.of(otherCommandMigration)
                    ),
                    false
            );

            assertNotSame(oldModel, tree.getModel());
            DefaultMutableTreeNode newRoot = root(tree);
            DefaultMutableTreeNode newCommandNode = child(newRoot, 1);
            assertSame(newCommand, newCommandNode.getUserObject());
            assertTrue(tree.isExpanded(new TreePath(newCommandNode.getPath())));
            assertNotNull(tree.getSelectionPath());
            assertSame(newSelected, ((DefaultMutableTreeNode) tree.getLastSelectedPathComponent()).getUserObject());
        });
    }

    private static JTree tree() {
        return new JTree(new DefaultTreeModel(new CheckedTreeNode()));
    }

    private static DefaultMutableTreeNode root(JTree tree) {
        return (DefaultMutableTreeNode) tree.getModel().getRoot();
    }

    private static DefaultMutableTreeNode child(DefaultMutableTreeNode parent, int index) {
        return (DefaultMutableTreeNode) parent.getChildAt(index);
    }

    private static MigrateCommand command(String name, boolean isDefault) {
        MigrateCommand command = new MigrateCommand();
        command.command = name;
        command.isDefault = isDefault;
        command.db = "db";
        command.migrationTable = "migration";
        return command;
    }

    private static Migration migration(String name, String path) {
        PhpClass phpClass = (PhpClass) Proxy.newProxyInstance(
                PhpClass.class.getClassLoader(),
                new Class<?>[]{PhpClass.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "getName" -> name;
                    case "getNamespaceName" -> "\\";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == arguments[0];
                    default -> defaultValue(method.getReturnType());
                }
        );
        return new Migration(phpClass, path);
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == char.class) {
            return '\0';
        }
        return 0;
    }
}
