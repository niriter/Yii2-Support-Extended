package com.nvlad.yii2support.i18n;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase;
import com.intellij.util.indexing.FileBasedIndex;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.nvlad.yii2support.i18n.inspections.Yii2NavigatorInterpolatedTranslationMessageInspection;
import com.nvlad.yii2support.i18n.inspections.Yii2NavigatorMissingTranslationsInspection;
import com.nvlad.yii2support.i18n.inspections.Yii2NavigatorUnknownTranslationInspection;
import com.nvlad.yii2support.i18n.inspections.Yii2NavigatorUnusedTranslationsInspection;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@SuppressWarnings("removal")
public class PhpI18nInspectionsPsiIntegrationTest extends LightPlatformCodeInsightFixtureTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        myFixture.addFileToProject("Yii.php", """
                <?php
                class Yii {
                    public static function t($category, $message, $params = [], $language = null) {}
                }
                """);
    }

    public void testStaticYiiCallResolvesAndUnknownKeyIsHighlighted() {
        addProvider("messages/en-US/app.php", """
                'known.key' => 'Known translation',
                """);
        PsiFile file = configureSource("""
                class Translator {
                    public static function t($category, $message) {}
                }

                Yii::t('app', 'known.key');
                Yii::t('app', 'unknown.key');
                Translator::t('app', 'unknown.other');
                """);

        TranslationCall call = TranslationCallResolver.resolve(findCall(file, "'known.key'"));
        assertNotNull(call);
        assertEquals("app", call.category());
        assertEquals("known.key", call.message());
        assertEquals("'known.key'", call.messageElement().getText());
        assertFalse(call.interpolated());
        assertNull(TranslationCallResolver.resolve(findCall(file, "'unknown.other'")));

        List<HighlightInfo> highlights = highlight(new Yii2NavigatorUnknownTranslationInspection());

        assertHighlighted(file, "'unknown.key'", highlights);
        assertNotHighlighted(file, "'known.key'", highlights);
        assertNotHighlighted(file, "'unknown.other'", highlights);
    }

    public void testInterpolatedTranslationMessageIsHighlighted() {
        PsiFile file = configureSource("""
                function welcome($name) {
                    return Yii::t('app', "Welcome, $name");
                }
                """);
        TranslationCall call = TranslationCallResolver.resolve(findCall(file, "\"Welcome, $name\""));

        assertNotNull(call);
        assertTrue(call.interpolated());

        List<HighlightInfo> highlights = highlight(new Yii2NavigatorInterpolatedTranslationMessageInspection());
        assertHighlighted(file, "\"Welcome, $name\"", highlights);
    }

    public void testUsedTranslationMissingFromOneProviderIsHighlighted() {
        myFixture.addFileToProject("src/Checkout.php", """
                <?php
                Yii::t('checkout', 'pay.now');
                """);
        addProvider("messages/en-US/checkout.php", """
                'pay.now' => 'Pay now',
                """);
        PsiFile incompleteProvider = addProvider("translations/de-DE/checkout.php", """
                'cancel' => 'Abbrechen',
                """);
        myFixture.configureFromExistingVirtualFile(incompleteProvider.getVirtualFile());

        List<HighlightInfo> highlights = highlight(new Yii2NavigatorMissingTranslationsInspection());

        assertHighlighted(incompleteProvider, "'cancel'", highlights);
        assertProblemMentions("pay.now", highlights);
    }

    public void testUnusedProviderKeyIsHighlighted() {
        myFixture.addFileToProject("src/Profile.php", """
                <?php
                Yii::t('profile', 'used.key');
                """);
        PsiFile provider = addProvider("messages/en-US/profile.php", """
                'used.key' => 'Used translation',
                'unused.key' => 'Unused translation',
                """);
        myFixture.configureFromExistingVirtualFile(provider.getVirtualFile());

        List<HighlightInfo> highlights = highlight(new Yii2NavigatorUnusedTranslationsInspection());

        assertHighlighted(provider, "'unused.key'", highlights);
        assertNotHighlighted(provider, "'used.key'", highlights);
    }

    public void testDynamicProviderKeyIsIgnoredConsistently() {
        PsiFile providerFile = myFixture.addFileToProject("messages/en-US/dynamic.php", """
                <?php
                $suffix = 'suffix';
                return [
                    "dynamic.$suffix" => 'Dynamic translation',
                    'static.key' => 'Static translation',
                ];
                """);

        TranslationProvider provider = TranslationProviderUtil.resolve(providerFile);

        assertNotNull(provider);
        assertEquals(Set.of("static.key"), provider.messages());

        myFixture.configureFromExistingVirtualFile(providerFile.getVirtualFile());
        List<HighlightInfo> highlights = highlight(new Yii2NavigatorUnusedTranslationsInspection());

        assertNotHighlighted(providerFile, "\"dynamic.$suffix\"", highlights);
        assertHighlighted(providerFile, "'static.key'", highlights);
    }

    public void testCompletionUsesIndexedProviderCatalog() {
        addProvider("messages/en-US/app.php", """
                'app.first' => 'First app message',
                'app.second' => 'Second app message',
                """);
        addProvider("translations/de-DE/admin.php", """
                'admin.create' => 'Create administrator',
                'admin.delete' => 'Delete administrator',
                """);

        myFixture.configureByText(
                PhpFileType.INSTANCE,
                "<?php Yii::t('<caret>', 'message');"
        );
        myFixture.completeBasic();

        List<String> categories = myFixture.getLookupElementStrings();
        assertNotNull(categories);
        assertContainsElements(categories, "admin", "app");

        myFixture.configureByText(
                PhpFileType.INSTANCE,
                "<?php Yii::t('admin', '<caret>');"
        );
        myFixture.completeBasic();

        List<String> messages = myFixture.getLookupElementStrings();
        assertNotNull(messages);
        assertContainsElements(messages, "admin.create", "admin.delete");
    }

    public void testTranslationKeyIndexOnlyAcceptsProviderPaths() {
        PsiFile provider = addProvider("messages/en-US/app.php", """
                'known.key' => 'Known translation',
                """);
        PsiFile source = myFixture.addFileToProject("src/Controller.php", "<?php class Controller {}");

        FileBasedIndex.InputFilter inputFilter = new TranslationKeyIndex().getInputFilter();

        assertTrue(inputFilter.acceptInput(provider.getVirtualFile()));
        assertFalse(inputFilter.acceptInput(source.getVirtualFile()));
    }

    public void testTranslationProviderResolverUsesOnlyTopLevelReturn() {
        PsiFile providerFile = myFixture.addFileToProject("translations/de-DE/account.php", """
                <?php
                function helper() {
                    return ['helper.key' => 'Not a translation'];
                }

                $helper = function () {
                    return ['closure.key' => 'Not a translation'];
                };

                if (true) {
                    return ['conditional.key' => 'Not a translation'];
                }

                return ['actual.translation' => 'Translation'];
                """);

        TranslationProvider provider = TranslationProviderUtil.resolve(providerFile);

        assertNotNull(provider);
        assertEquals("account", provider.category());
        assertEquals(Set.of("actual.translation"), provider.messages());
        assertNotNull(provider.array());
    }

    public void testTranslationProviderResolverRejectsConditionalReturn() {
        PsiFile providerFile = myFixture.addFileToProject("messages/en-US/conditional.php", """
                <?php
                if (true) {
                    return ['conditional.key' => 'Not a translation provider'];
                }
                """);

        assertNull(TranslationProviderUtil.resolve(providerFile));
    }

    private PsiFile configureSource(String body) {
        return myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" + body);
    }

    private PsiFile addProvider(String path, String entries) {
        return myFixture.addFileToProject(path, "<?php\nreturn [" + entries + "];\n");
    }

    private List<HighlightInfo> highlight(LocalInspectionTool inspection) {
        myFixture.enableInspections(inspection);
        return myFixture.doHighlighting();
    }

    private MethodReference findCall(PsiFile file, String messageText) {
        Collection<MethodReference> references = PsiTreeUtil.findChildrenOfType(
                file,
                MethodReference.class
        );
        for (MethodReference reference : references) {
            if (reference.getParameters().length > 1
                    && messageText.equals(reference.getParameters()[1].getText())) {
                return reference;
            }
        }

        fail("Translation call not found for message: " + messageText);
        return null;
    }

    private void assertHighlighted(PsiFile file, String text, List<HighlightInfo> highlights) {
        int start = file.getText().indexOf(text);
        assertTrue("Text not found in fixture: " + text, start >= 0);
        int end = start + text.length();

        for (HighlightInfo highlight : highlights) {
            if (highlight.getStartOffset() < end && highlight.getEndOffset() > start) {
                return;
            }
        }

        fail("Expected an inspection highlight at: " + text);
    }

    private void assertNotHighlighted(PsiFile file, String text, List<HighlightInfo> highlights) {
        int start = file.getText().indexOf(text);
        assertTrue("Text not found in fixture: " + text, start >= 0);
        int end = start + text.length();

        for (HighlightInfo highlight : highlights) {
            if (highlight.getStartOffset() < end && highlight.getEndOffset() > start) {
                fail("Unexpected inspection highlight at " + text + ": " + highlight.getDescription());
            }
        }
    }

    private void assertProblemMentions(String text, List<HighlightInfo> highlights) {
        for (HighlightInfo highlight : highlights) {
            String description = highlight.getDescription();
            if (description != null && description.contains(text)) {
                return;
            }
        }

        fail("Expected an inspection problem mentioning: " + text);
    }
}
