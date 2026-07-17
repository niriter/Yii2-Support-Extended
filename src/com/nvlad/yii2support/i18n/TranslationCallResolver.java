package com.nvlad.yii2support.i18n;

import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.ClassReference;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.PhpExpression;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import com.jetbrains.php.util.PhpStringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Resolves the PHP-only Yii::t() shape supported by the MVP.
 */
public final class TranslationCallResolver {
    private TranslationCallResolver() {
    }

    public static boolean isTranslationMethod(@NotNull MethodReference reference) {
        if (!reference.isStatic() || !"t".equals(reference.getName())) {
            return false;
        }

        PhpExpression classReference = reference.getClassReference();
        return classReference instanceof ClassReference && "Yii".equals(classReference.getName());
    }

    @Nullable
    static String resolveStaticCategory(@Nullable PsiElement element) {
        if (!(element instanceof StringLiteralExpression categoryElement)
                || categoryElement.getChildren().length != 0) {
            return null;
        }

        String category = PhpStringUtil.unescapeText(
                categoryElement.getContents(),
                categoryElement.isSingleQuote()
        );
        if (category.isEmpty()) {
            return null;
        }

        int categorySeparator = category.lastIndexOf('/');
        if (categorySeparator >= 0) {
            category = category.substring(categorySeparator + 1);
        }
        return category.isEmpty() ? null : category;
    }

    @Nullable
    public static TranslationCall resolve(@NotNull MethodReference reference) {
        if (!isTranslationMethod(reference)) {
            return null;
        }

        PsiElement[] parameters = reference.getParameters();
        if (parameters.length < 2 || !(parameters[1] instanceof StringLiteralExpression messageElement)) {
            return null;
        }

        String category = resolveStaticCategory(parameters[0]);
        if (category == null) {
            return null;
        }

        boolean interpolated = messageElement.getChildren().length != 0;
        String message = PhpStringUtil.unescapeText(messageElement.getContents(), messageElement.isSingleQuote());
        if (message.isEmpty()) {
            return null;
        }

        return new TranslationCall(
                category,
                message,
                messageElement,
                interpolated
        );
    }
}
