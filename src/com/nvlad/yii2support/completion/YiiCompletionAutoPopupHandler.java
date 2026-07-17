package com.nvlad.yii2support.completion;

import com.intellij.codeInsight.AutoPopupController;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ArrayUtil;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.ArrayAccessExpression;
import com.jetbrains.php.lang.psi.elements.ArrayCreationExpression;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.ParameterList;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import com.nvlad.yii2support.common.ClassUtils;
import com.nvlad.yii2support.i18n.TranslationCallResolver;
import com.nvlad.yii2support.views.util.ViewUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Schedules Yii-specific completion popups for punctuation typed in PHP files.
 */
public final class YiiCompletionAutoPopupHandler extends TypedHandlerDelegate {
    @Override
    public @NotNull Result checkAutoPopup(
            char charTyped,
            @NotNull Project project,
            @NotNull Editor editor,
            @NotNull PsiFile file
    ) {
        if (!(file instanceof PhpFile) || !isTriggerCharacter(charTyped)) {
            return Result.CONTINUE;
        }

        PsiElement position = findPositionAtCaret(file, editor);
        if (position == null || !shouldAutoPopup(position, charTyped)) {
            return Result.CONTINUE;
        }

        AutoPopupController.getInstance(project)
                .scheduleAutoPopup(editor, CompletionType.BASIC, null);
        return Result.STOP;
    }

    private static boolean isTriggerCharacter(char charTyped) {
        return charTyped == '\'' || charTyped == '"' || charTyped == '.' || charTyped == '@';
    }

    @Nullable
    private static PsiElement findPositionAtCaret(@NotNull PsiFile file, @NotNull Editor editor) {
        int offset = editor.getCaretModel().getOffset();
        if (offset < file.getTextLength()) {
            return file.findElementAt(offset);
        }
        return offset > 0 ? file.findElementAt(offset - 1) : null;
    }

    private static boolean shouldAutoPopup(@NotNull PsiElement position, char charTyped) {
        if (charTyped == '\'' || charTyped == '"') {
            if (isTranslationArgument(position) || isViewArgument(position, charTyped)) {
                return true;
            }

            PsiElement parent = position.getParent();
            if (parent instanceof ArrayCreationExpression
                    || parent instanceof ArrayAccessExpression
                    || parent instanceof MethodReference
                    || isMethodReferenceArgument(position)) {
                return true;
            }
        }

        if (charTyped == '@' && isViewArgument(position, charTyped)) {
            return true;
        }

        return (charTyped == '\'' || charTyped == '"' || charTyped == '.')
                && isQueryCall(position);
    }

    private static boolean isTranslationArgument(@NotNull PsiElement position) {
        MethodReference reference = PsiTreeUtil.getParentOfType(position, MethodReference.class);
        if (reference == null || !TranslationCallResolver.isTranslationMethod(reference)) {
            return false;
        }

        if (position instanceof LeafPsiElement
                && ("$category".equals(position.getText()) || "$message".equals(position.getText()))) {
            return true;
        }
        return position.getNextSibling() instanceof ParameterList
                || isParameterListOf(position, reference);
    }

    private static boolean isViewArgument(@NotNull PsiElement position, char charTyped) {
        MethodReference reference = PsiTreeUtil.getParentOfType(position, MethodReference.class);
        if (reference == null || !ArrayUtil.contains(reference.getName(), ViewUtil.renderMethods)) {
            return false;
        }

        if (charTyped == '\'' || charTyped == '"') {
            if (position instanceof LeafPsiElement && "$view".equals(position.getText())) {
                return true;
            }
            return position.getNextSibling() instanceof ParameterList
                    || isParameterListOf(position, reference);
        }

        if (charTyped != '@' || !(position.getParent() instanceof StringLiteralExpression)) {
            return false;
        }

        ParameterList parameterList = PsiTreeUtil.getParentOfType(position, ParameterList.class);
        PsiElement[] parameters = parameterList == null ? PsiElement.EMPTY_ARRAY : parameterList.getParameters();
        return parameters.length > 0 && parameters[0] == position.getParent();
    }

    private static boolean isMethodReferenceArgument(@NotNull PsiElement position) {
        return position.getParent() instanceof ParameterList parameterList
                && parameterList.getParent() instanceof MethodReference;
    }

    private static boolean isParameterListOf(
            @NotNull PsiElement position,
            @NotNull MethodReference reference
    ) {
        return position.getParent() instanceof ParameterList parameterList
                && parameterList.getParent() == reference;
    }

    private static boolean isQueryCall(@NotNull PsiElement position) {
        MethodReference methodReference = ClassUtils.getMethodRef(position, 10);
        if (methodReference == null || !(methodReference.resolve() instanceof Method method)) {
            return false;
        }

        if (!(method.getParent() instanceof PhpClass containingClass)) {
            return false;
        }

        PhpIndex index = PhpIndex.getInstance(method.getProject());
        return ClassUtils.isClassInheritsOrEqual(
                containingClass,
                ClassUtils.getClass(index, "\\yii\\db\\Query"),
                100
        ) || ClassUtils.isClassInheritsOrEqual(
                containingClass,
                ClassUtils.getClass(index, "\\yii\\db\\Command"),
                100
        ) || ClassUtils.isClassInherit(
                containingClass,
                ClassUtils.getClass(index, "\\yii\\db\\BaseActiveRecord")
        ) || ClassUtils.isClassInheritsOrEqual(
                containingClass,
                ClassUtils.getClass(index, "\\yii\\db\\Migration"),
                100
        );
    }
}
