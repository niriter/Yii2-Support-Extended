package com.nvlad.yii2support.attributeLabels;

import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.patterns.PlatformPatterns;

public class AttributeLabelCompletionContributor extends com.intellij.codeInsight.completion.CompletionContributor{
    public AttributeLabelCompletionContributor() {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), new AttributeLabelCompletionProvider());
    }
}
