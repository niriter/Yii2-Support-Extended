package com.nvlad.yii2support.objectfactory;

import com.intellij.openapi.util.TextRange;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.psi.elements.ArrayCreationExpression;
import com.jetbrains.php.lang.psi.elements.ArrayHashElement;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpClassMember;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import com.nvlad.yii2support.common.ClassUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Adds navigation from a confirmed Yii Object Factory configuration key to
 * the writable PHP field or setter that accepts that key.
 */
public final class ObjectFactoryReferenceContributor extends PsiReferenceContributor {
    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
        registrar.registerReferenceProvider(
                PlatformPatterns.psiElement(StringLiteralExpression.class),
                new PsiReferenceProvider() {
                    @Override
                    public PsiReference @NotNull [] getReferencesByElement(
                            @NotNull PsiElement element,
                            @NotNull ProcessingContext context
                    ) {
                        return createReferences(element);
                    }
                }
        );
    }

    private static PsiReference @NotNull [] createReferences(@NotNull PsiElement element) {
        if (!(element instanceof StringLiteralExpression)) {
            return PsiReference.EMPTY_ARRAY;
        }

        StringLiteralExpression key = (StringLiteralExpression) element;
        ArrayHashElement hashElement = PsiTreeUtil.getParentOfType(key, ArrayHashElement.class, true);
        if (hashElement == null) {
            return PsiReference.EMPTY_ARRAY;
        }

        if (hashElement.getKey() != key || !(hashElement.getParent() instanceof ArrayCreationExpression)) {
            return PsiReference.EMPTY_ARRAY;
        }

        PsiFile file = key.getContainingFile();
        if (file == null) {
            return PsiReference.EMPTY_ARRAY;
        }

        ArrayCreationExpression array = (ArrayCreationExpression) hashElement.getParent();
        ObjectFactoryContext factoryContext = ObjectFactoryUtils.resolveContext(
                array,
                file.getContainingDirectory()
        );
        PhpClass targetClass = factoryContext.getTargetClass();
        PhpClassMember targetMember = ClassUtils.findWritableField(targetClass, key.getContents());
        if (targetMember == null) {
            return PsiReference.EMPTY_ARRAY;
        }

        TextRange rangeInElement = ElementManipulators.getValueTextRange(key);
        PsiReference reference = new PsiReferenceBase.Immediate<>(
                key,
                rangeInElement,
                true,
                targetMember
        );
        return new PsiReference[]{reference};
    }
}
