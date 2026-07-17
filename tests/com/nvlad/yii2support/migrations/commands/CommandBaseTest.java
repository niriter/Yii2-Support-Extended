package com.nvlad.yii2support.migrations.commands;

import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.nvlad.yii2support.migrations.entities.Migration;
import com.nvlad.yii2support.migrations.entities.MigrationStatus;
import junit.framework.TestCase;

import javax.swing.tree.DefaultMutableTreeNode;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.Date;

public class CommandBaseTest extends TestCase {
    public void testFindsLogicallySameMigrationInRebuiltNestedTree() {
        Migration commandMigration = migration("m240201_000000_selected", "\\tenant", "tenant/migrations");
        Migration rebuiltMigration = migration("m240201_000000_selected", "\\tenant", "tenant/migrations");

        DefaultMutableTreeNode root = new DefaultMutableTreeNode();
        DefaultMutableTreeNode commandNode = new DefaultMutableTreeNode("command");
        DefaultMutableTreeNode pathNode = new DefaultMutableTreeNode("tenant/migrations");
        DefaultMutableTreeNode migrationNode = new DefaultMutableTreeNode(rebuiltMigration);
        root.add(commandNode);
        commandNode.add(pathNode);
        pathNode.add(migrationNode);

        assertSame(migrationNode, CommandBase.findMigrationNode(root, commandMigration));
        assertNull(CommandBase.findMigrationNode(
                root,
                migration("m240201_000000_selected", "\\other", "tenant/migrations")
        ));
    }

    public void testCopiesStatusRenderedByReplacementNode() {
        Migration source = migration("m240201_000000_selected", "\\tenant", "tenant/migrations");
        Migration displayed = migration("m240201_000000_selected", "\\tenant", "tenant/migrations");
        source.status = MigrationStatus.Progress;
        source.createdAt = new Date(1_000L);
        source.applyAt = new Date(2_000L);
        source.downDuration = Duration.ofSeconds(3);
        source.upDuration = Duration.ofSeconds(4);

        CommandBase.copyDisplayState(source, displayed);

        assertEquals(source.status, displayed.status);
        assertEquals(source.createdAt, displayed.createdAt);
        assertEquals(source.applyAt, displayed.applyAt);
        assertEquals(source.downDuration, displayed.downDuration);
        assertEquals(source.upDuration, displayed.upDuration);
    }

    private static Migration migration(String name, String namespace, String path) {
        PhpClass phpClass = (PhpClass) Proxy.newProxyInstance(
                PhpClass.class.getClassLoader(),
                new Class<?>[]{PhpClass.class},
                (proxy, method, arguments) -> {
                    switch (method.getName()) {
                        case "getName":
                            return name;
                        case "getNamespaceName":
                            return namespace;
                        case "hashCode":
                            return System.identityHashCode(proxy);
                        case "equals":
                            return proxy == arguments[0];
                        default:
                            return defaultValue(method.getReturnType());
                    }
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
