package com.nvlad.yii2support.i18n.inspections;

import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElementVisitor;
import com.jetbrains.php.lang.inspections.PhpInspection;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.visitors.PhpElementVisitor;
import com.nvlad.yii2support.i18n.TranslationCall;
import com.nvlad.yii2support.i18n.TranslationCallResolver;
import org.jetbrains.annotations.NotNull;

public class Yii2NavigatorInterpolatedTranslationMessageInspection extends PhpInspection {
    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder problemsHolder, boolean isOnTheFly) {
        return new PhpElementVisitor() {
            @Override
            public void visitPhpMethodReference(MethodReference reference) {
                TranslationCall call = TranslationCallResolver.resolve(reference);
                if (call != null && call.interpolated()) {
                    problemsHolder.registerProblem(
                            call.messageElement(),
                            "Translation message is interpolated",
                            ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                    );
                }
            }
        };
    }
}
