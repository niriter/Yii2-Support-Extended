package com.nvlad.yii2support.i18n;

import com.intellij.codeInspection.LocalInspectionTool;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class InspectionRegistrationTest {
    @Test
    public void localInspectionMetadataMatchesImplementation() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        try (InputStream pluginXml = classLoader.getResourceAsStream("META-INF/plugin.xml")) {
            assertNotNull("META-INF/plugin.xml is missing from test resources", pluginXml);

            Document document = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(pluginXml);
            NodeList inspections = document.getElementsByTagName("localInspection");

            for (int i = 0; i < inspections.getLength(); i++) {
                Element registration = (Element) inspections.item(i);
                String implementationClass = registration.getAttribute("implementationClass");
                String registeredShortName = registration.getAttribute("shortName");
                LocalInspectionTool inspection = (LocalInspectionTool) Class.forName(implementationClass)
                        .getDeclaredConstructor()
                        .newInstance();

                assertEquals(
                        implementationClass + " has a mismatched shortName",
                        registeredShortName,
                        inspection.getShortName()
                );
                assertNotNull(
                        "Missing inspection description for " + registeredShortName,
                        classLoader.getResource(
                                "inspectionDescriptions/" + registeredShortName + ".html"
                        )
                );
            }
        }
    }
}
