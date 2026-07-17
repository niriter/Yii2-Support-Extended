package com.nvlad.yii2support.forms;

import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.ArrayCreationExpression;

/**
 * Created by oleg on 2017-12-18.
 */
public class FieldAttributesCompletionContributor extends com.intellij.codeInsight.completion.CompletionContributor {
    public FieldAttributesCompletionContributor() {
        extend(CompletionType.BASIC, ElementPattern(), new FieldAttributesCompletionProvider());
    }
    private static ElementPattern<PsiElement> ElementPattern() {
        return
                PlatformPatterns.or(
                        PlatformPatterns.psiElement().withSuperParent(3, ArrayCreationExpression.class),
                        PlatformPatterns.psiElement().withSuperParent(4, ArrayCreationExpression.class));
    }
}
