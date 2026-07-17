package com.nvlad.yii2support.views.smarty;

import com.intellij.openapi.fileTypes.FileType;
import junit.framework.TestCase;

import java.lang.reflect.Proxy;

public class SmartyViewFileTypeSupportTest extends TestCase {
    private final SmartyViewFileTypeSupport support = new SmartyViewFileTypeSupport();

    public void testRecognizesSmartyByRegisteredFileTypeName() {
        assertTrue(support.supports(fileType("Smarty")));
        assertFalse(support.supports(fileType("PHP")));
    }

    public void testProvidesSmartyViewTemplate() {
        assertEquals("Yii2 Smarty View File", support.getTemplateName());
    }

    private static FileType fileType(String name) {
        return (FileType) Proxy.newProxyInstance(
                FileType.class.getClassLoader(),
                new Class<?>[]{FileType.class},
                (proxy, method, arguments) -> method.getName().equals("getName") ? name : null
        );
    }
}
