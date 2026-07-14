package com.nvlad.yii2support;

import junit.framework.TestCase;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;

/**
 * Checks the descriptor shipped on the plugin runtime classpath without
 * starting the complete PhpStorm application and its unrelated services.
 */
public class PluginDescriptorTest extends TestCase {
    public void testDescriptorHasExpectedIdentityAndPhpDependency() throws Exception {
        try (InputStream descriptorStream = getClass().getResourceAsStream("/META-INF/plugin.xml")) {
            assertNotNull("META-INF/plugin.xml must be available on the plugin classpath", descriptorStream);

            Document descriptor = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(descriptorStream);
            Element plugin = descriptor.getDocumentElement();

            assertEquals("idea-plugin", plugin.getTagName());
            assertEquals("com.yii2support", plugin.getElementsByTagName("id").item(0).getTextContent().trim());
            assertEquals("Yii2 Support", plugin.getElementsByTagName("name").item(0).getTextContent().trim());
            assertTrue("PHP plugin dependency must be declared", hasDependency(plugin, "com.jetbrains.php"));
            assertEquals("legacy application components must not be registered", 0,
                    plugin.getElementsByTagName("application-components").getLength());
            assertEquals("startup activity must be registered exactly once", 1,
                    plugin.getElementsByTagName("postStartupActivity").getLength());
        }
    }

    private static boolean hasDependency(Element plugin, String pluginId) {
        var dependencies = plugin.getElementsByTagName("depends");
        for (int index = 0; index < dependencies.getLength(); index++) {
            if (pluginId.equals(dependencies.item(index).getTextContent().trim())) {
                return true;
            }
        }
        return false;
    }
}
