package com.nvlad.yii2support.database;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.openapi.util.Pair;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.util.containers.MultiMap;
import com.jetbrains.php.lang.psi.elements.*;

public class QueryCompletionContributor extends com.intellij.codeInsight.completion.CompletionContributor {
    private final MultiMap<CompletionType, Pair<ElementPattern<? extends PsiElement>, CompletionProvider<CompletionParameters>>> myMap = new MultiMap<>();

    public QueryCompletionContributor() {
        extend(CompletionType.BASIC, ElementPattern(), new QueryCompletionProvider());
    }
    private static ElementPattern<PsiElement> ElementPattern() {

         return
                 PlatformPatterns.or(
                         // ["<caret>
                     PlatformPatterns.psiElement().withSuperParent(3, ArrayCreationExpression.class),
                         // string
                     PlatformPatterns.psiElement().withSuperParent(3, MethodReference.class).withParent(StringLiteralExpression.class),
                         // ["<caret>" => ""]
                         PlatformPatterns.psiElement().withSuperParent(4, ArrayCreationExpression.class)

                 );


    }
}
