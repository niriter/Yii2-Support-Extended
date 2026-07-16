package com.nvlad.yii2support.i18n;

import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import org.jetbrains.annotations.NotNull;

/**
 * The static parts of a Yii translation call resolved from PHP PSI.
 */
public record TranslationCall(
        @NotNull String category,
        @NotNull String message,
        @NotNull StringLiteralExpression messageElement,
        boolean interpolated
) {
}
