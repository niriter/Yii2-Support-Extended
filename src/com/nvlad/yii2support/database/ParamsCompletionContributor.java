package com.nvlad.yii2support.database;


import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.ArrayCreationExpression;

public class ParamsCompletionContributor  extends com.intellij.codeInsight.completion.CompletionContributor  {
    public ParamsCompletionContributor() {
        extend(CompletionType.BASIC, ElementPattern(), new ParamsCompletionProvider());
    }
    private static ElementPattern<PsiElement> ElementPattern() {
        return
                PlatformPatterns.or(
                    PlatformPatterns.psiElement().withSuperParent(3, ArrayCreationExpression.class),
                        PlatformPatterns.psiElement().withSuperParent(4, ArrayCreationExpression.class));
    }

}
