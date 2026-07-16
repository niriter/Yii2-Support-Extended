package com.nvlad.yii2support.i18n.inspections;

import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElementVisitor;
import com.jetbrains.php.lang.inspections.PhpInspection;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.visitors.PhpElementVisitor;
import com.nvlad.yii2support.i18n.TranslationCall;
import com.nvlad.yii2support.i18n.TranslationCallResolver;
import com.nvlad.yii2support.i18n.TranslationKeyIndex;
import org.jetbrains.annotations.NotNull;

public class Yii2NavigatorUnknownTranslationInspection extends PhpInspection {
    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder problemsHolder, boolean isOnTheFly) {
        return new PhpElementVisitor() {
            @Override
            public void visitPhpMethodReference(MethodReference reference) {
                Project project = reference.getProject();
                if (DumbService.isDumb(project)) {
                    return;
                }

                TranslationCall call = TranslationCallResolver.resolve(reference);
                if (call == null || call.interpolated()) {
                    return;
                }

                if (!TranslationKeyIndex.contains(project, call.category(), call.message())) {
                    problemsHolder.registerProblem(
                            call.messageElement(),
                            "Unknown translation '" + call.message() + "'",
                            ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                    );
                }
            }
        };
    }
}
