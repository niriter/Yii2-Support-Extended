package com.nvlad.yii2support.completion;

import com.intellij.codeInsight.editorActions.TypedHandlerDelegate;
import com.intellij.testFramework.EdtTestUtil;
import com.intellij.testFramework.fixtures.CompletionAutoPopupTestCase;
import com.jetbrains.php.lang.PhpFileType;

import java.util.List;

public class YiiCompletionAutoPopupHandlerPsiIntegrationTest extends CompletionAutoPopupTestCase {
    private final YiiCompletionAutoPopupHandler handler = new YiiCompletionAutoPopupHandler();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        myFixture.addFileToProject("Yii.php", """
                <?php
                class Yii {
                    public static function t($category, $message, $params = [], $language = null) {}
                }
                """);
        myFixture.addFileToProject("yii/db/Query.php", """
                <?php
                namespace yii\\db;
                class Query {
                    public function select($columns) {}
                }
                """);
    }

    public void testRegisteredHandlerOpensI18nLookupAfterTypingQuote() {
        myFixture.addFileToProject("messages/en-US/app.php", """
                <?php
                return ['known.key' => 'Known translation'];
                """);
        configure("Yii::t(<caret>, 'message');");

        myFixture.type('\'');
        myTester.joinAutopopup();
        myTester.joinCompletion();

        assertNotNull("Typing a quote must open the registered completion lookup", myFixture.getLookup());
        List<String> lookupStrings = myFixture.getLookupElementStrings();
        assertNotNull(lookupStrings);
        assertContainsElements(lookupStrings, "app");
    }

    public void testQuoteTriggersAutoPopupInArray() {
        configure("$config = [<caret>];");

        assertEquals(TypedHandlerDelegate.Result.STOP, check('\''));
    }

    public void testQuoteDoesNotTriggerAutoPopupInUnrelatedStatement() {
        configure("<caret>;");

        assertEquals(TypedHandlerDelegate.Result.CONTINUE, check('\''));
    }

    public void testAtSignTriggersAutoPopupInFirstRenderArgument() {
        configure("$controller->render('<caret>');");

        assertEquals(TypedHandlerDelegate.Result.STOP, check('@'));
    }

    public void testDotTriggersAutoPopupInQueryMethodArgument() {
        configure("""
                $query = new \\yii\\db\\Query();
                $query->select('user<caret>');
                """);

        assertEquals(TypedHandlerDelegate.Result.STOP, check('.'));
    }

    public void testQuoteTriggersAutoPopupInsideMethodReference() {
        configure("$model->get(<caret>);");

        assertEquals(TypedHandlerDelegate.Result.STOP, check('\''));
    }

    public void testQuoteDoesNotTriggerForNestedStringInUnrelatedMethodReference() {
        configure("$model->get($formatter->format('value<caret>'));");

        assertEquals(TypedHandlerDelegate.Result.CONTINUE, check('\''));
    }

    private void configure(String body) {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" + body);
    }

    private TypedHandlerDelegate.Result check(char charTyped) {
        return EdtTestUtil.runInEdtAndGet(
                () -> handler.checkAutoPopup(
                        charTyped,
                        getProject(),
                        myFixture.getEditor(),
                        myFixture.getFile()
                )
        );
    }
}
