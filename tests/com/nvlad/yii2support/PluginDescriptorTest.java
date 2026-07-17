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
            assertEquals("com.meekitak.yii2navigator", plugin.getElementsByTagName("id").item(0).getTextContent().trim());
            assertEquals("Yii2 Navigator", plugin.getElementsByTagName("name").item(0).getTextContent().trim());
            assertEquals("meekitak", plugin.getElementsByTagName("vendor").item(0).getTextContent().trim());
            assertEquals("com.yii2support",
                    plugin.getElementsByTagName("incompatible-with").item(0).getTextContent().trim());
            assertTrue("PHP plugin dependency must be declared", hasDependency(plugin, "com.jetbrains.php"));
            Element twigDependency = findDependency(plugin, "com.jetbrains.twig");
            assertNotNull("Twig integration must be declared as optional", twigDependency);
            assertEquals("true", twigDependency.getAttribute("optional"));
            assertEquals("twig.xml", twigDependency.getAttribute("config-file"));
            assertEquals("legacy application components must not be registered", 0,
                    plugin.getElementsByTagName("application-components").getLength());
            assertEquals("version notification startup activity must not be registered", 0,
                    countExtensions(
                            plugin,
                            "postStartupActivity",
                            "implementation",
                            "com.nvlad.yii2support.PluginStartupActivity"
                    ));
            assertEquals("version notification settings must not be registered", 0,
                    countExtensions(
                            plugin,
                            "applicationService",
                            "serviceImplementation",
                            "com.nvlad.yii2support.PluginGlobalSettings"
                    ));
            assertEquals("version notification group must not be registered", 0,
                    countExtensions(plugin, "notificationGroup", "id", "Yii2 Navigator"));
            assertEquals("obsolete error submitter must not be registered", 0,
                    plugin.getElementsByTagName("errorHandler").getLength());
            assertEquals("Object Factory reference contributor must be registered exactly once", 1,
                    countExtensions(
                            plugin,
                            "psi.referenceContributor",
                            "implementation",
                            "com.nvlad.yii2support.objectfactory.ObjectFactoryReferenceContributor"
                    ));
            assertEquals("Smarty view support must be registered exactly once", 1,
                    countExtensions(
                            plugin,
                            "viewFileTypeSupport",
                            "implementation",
                            "com.nvlad.yii2support.views.smarty.SmartyViewFileTypeSupport"
                    ));
        }
    }

    public void testMarketplaceResourcesArePackaged() {
        assertNotNull("pluginIcon.svg must be available on the plugin classpath",
                getClass().getResource("/META-INF/pluginIcon.svg"));
        assertNotNull("BSD license must be included in the plugin distribution",
                getClass().getResource("/META-INF/LICENSE.md"));
    }

    public void testOptionalTwigDescriptorRegistersFileTypeSupport() throws Exception {
        try (InputStream descriptorStream = getClass().getResourceAsStream("/META-INF/twig.xml")) {
            assertNotNull("META-INF/twig.xml must be available on the plugin classpath", descriptorStream);

            Document descriptor = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(descriptorStream);
            assertEquals(1, countExtensions(
                    descriptor.getDocumentElement(),
                    "viewFileTypeSupport",
                    "implementation",
                    "com.nvlad.yii2support.views.twig.TwigViewFileTypeSupport"
            ));
        }
    }

    private static boolean hasDependency(Element plugin, String pluginId) {
        return findDependency(plugin, pluginId) != null;
    }

    private static Element findDependency(Element plugin, String pluginId) {
        var dependencies = plugin.getElementsByTagName("depends");
        for (int index = 0; index < dependencies.getLength(); index++) {
            if (pluginId.equals(dependencies.item(index).getTextContent().trim())) {
                return (Element) dependencies.item(index);
            }
        }
        return null;
    }

    private static int countExtensions(
            Element plugin,
            String elementName,
            String attributeName,
            String attributeValue
    ) {
        var extensions = plugin.getElementsByTagName(elementName);
        int count = 0;
        for (int index = 0; index < extensions.getLength(); index++) {
            Element extension = (Element) extensions.item(index);
            if (attributeValue.equals(extension.getAttribute(attributeName))) {
                count++;
            }
        }
        return count;
    }
}
