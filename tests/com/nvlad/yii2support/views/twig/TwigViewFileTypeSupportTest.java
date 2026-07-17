package com.nvlad.yii2support.views.twig;

import com.intellij.openapi.fileTypes.FileType;
import junit.framework.TestCase;

import java.lang.reflect.Proxy;

public class TwigViewFileTypeSupportTest extends TestCase {
    private final TwigViewFileTypeSupport support = new TwigViewFileTypeSupport();

    public void testRecognizesTwigByRegisteredFileTypeName() {
        assertTrue(support.supports(fileType("Twig")));
        assertFalse(support.supports(fileType("PHP")));
    }

    public void testProvidesTwigViewTemplate() {
        assertEquals("Yii2 Twig View File", support.getTemplateName());
    }

    private static FileType fileType(String name) {
        return (FileType) Proxy.newProxyInstance(
                FileType.class.getClassLoader(),
                new Class<?>[]{FileType.class},
                (proxy, method, arguments) -> method.getName().equals("getName") ? name : null
        );
    }
}
