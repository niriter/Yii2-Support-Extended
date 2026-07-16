package com.nvlad.yii2support.i18n;

import com.jetbrains.php.lang.psi.elements.ArrayCreationExpression;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Resolved data from a standard Yii translation provider.
 */
public record TranslationProvider(
        @NotNull String category,
        @NotNull ArrayCreationExpression array,
        @NotNull Set<String> messages
) {
    public TranslationProvider {
        messages = Set.copyOf(messages);
    }
}
