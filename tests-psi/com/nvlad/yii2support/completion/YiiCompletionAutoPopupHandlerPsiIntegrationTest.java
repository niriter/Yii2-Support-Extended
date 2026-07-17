package com.nvlad.yii2support.completion;

import com.intellij.codeInsight.editorActions.TypedHandlerDelegate;
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase;
import com.jetbrains.php.lang.PhpFileType;

@SuppressWarnings("removal")
public class YiiCompletionAutoPopupHandlerPsiIntegrationTest extends LightPlatformCodeInsightFixtureTestCase {
    private final YiiCompletionAutoPopupHandler handler = new YiiCompletionAutoPopupHandler();

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

    private void configure(String body) {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" + body);
    }

    private TypedHandlerDelegate.Result check(char charTyped) {
        return handler.checkAutoPopup(
                charTyped,
                getProject(),
                myFixture.getEditor(),
                myFixture.getFile()
        );
    }
}
