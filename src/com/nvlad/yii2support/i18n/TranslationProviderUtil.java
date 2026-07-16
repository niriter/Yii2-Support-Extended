package com.nvlad.yii2support.i18n;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.jetbrains.php.codeInsight.PhpScopeHolder;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.ArrayCreationExpression;
import com.jetbrains.php.lang.psi.elements.ArrayHashElement;
import com.jetbrains.php.lang.psi.elements.GroupStatement;
import com.jetbrains.php.lang.psi.elements.PhpReturn;
import com.jetbrains.php.lang.psi.elements.Statement;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import com.jetbrains.php.util.PhpStringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilities for standard Yii PHP translation providers.
 */
public final class TranslationProviderUtil {
    private static final Pattern PROVIDER_PATH = Pattern.compile(
            "(?:^|/)(?:messages|translations)/[^/]+/([^/]+)\\.php$"
    );

    private TranslationProviderUtil() {
    }

    static boolean isProviderPath(@NotNull String path) {
        return findProviderPath(path) != null;
    }

    @Nullable
    public static TranslationProvider resolve(@NotNull PsiFile file) {
        if (!(file instanceof PhpFile)
                || file.getFileType() != PhpFileType.INSTANCE
                || file.getVirtualFile() == null) {
            return null;
        }

        Matcher matcher = findProviderPath(file.getVirtualFile().getPath());
        if (matcher == null) {
            return null;
        }

        ArrayCreationExpression array = findTranslationArray(file);
        if (array == null) {
            return null;
        }

        return new TranslationProvider(matcher.group(1), array, collectMessages(array));
    }

    @Nullable
    public static String resolveStaticMessageKey(@Nullable PsiElement key) {
        if (!(key instanceof StringLiteralExpression literal)
                || literal.getChildren().length != 0) {
            return null;
        }

        return PhpStringUtil.unescapeText(literal.getContents(), literal.isSingleQuote());
    }

    @NotNull
    private static Set<String> collectMessages(@NotNull ArrayCreationExpression array) {
        Set<String> messages = new HashSet<>();
        for (ArrayHashElement element : array.getHashElements()) {
            String message = resolveStaticMessageKey(element.getKey());
            if (message != null) {
                messages.add(message);
            }
        }
        return messages;
    }

    @Nullable
    private static ArrayCreationExpression findTranslationArray(@NotNull PsiElement root) {
        return findTranslationArray(root, true);
    }

    @Nullable
    private static ArrayCreationExpression findTranslationArray(
            @NotNull PsiElement root,
            boolean allowRootGroup
    ) {
        for (PsiElement child : root.getChildren()) {
            ArrayCreationExpression array = getReturnedArray(child);
            if (array != null) {
                return array;
            }

            if (child instanceof PhpScopeHolder) {
                continue;
            }

            if (child instanceof GroupStatement) {
                if (!allowRootGroup) {
                    continue;
                }
                array = findTranslationArray(child, false);
            } else if (child instanceof Statement) {
                continue;
            } else {
                array = findTranslationArray(child, allowRootGroup);
            }

            if (array != null) {
                return array;
            }
        }

        return null;
    }

    @Nullable
    private static Matcher findProviderPath(@NotNull String path) {
        Matcher matcher = PROVIDER_PATH.matcher(path);
        return matcher.find() ? matcher : null;
    }

    @Nullable
    private static ArrayCreationExpression getReturnedArray(@NotNull PsiElement element) {
        if (!(element instanceof PhpReturn phpReturn)) {
            return null;
        }

        PsiElement value = phpReturn.getFirstPsiChild();
        return value instanceof ArrayCreationExpression ? (ArrayCreationExpression) value : null;
    }
}
