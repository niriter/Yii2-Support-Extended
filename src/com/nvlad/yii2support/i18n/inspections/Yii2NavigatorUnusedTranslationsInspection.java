package com.nvlad.yii2support.i18n.inspections;

import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.jetbrains.php.lang.inspections.PhpInspection;
import com.jetbrains.php.lang.psi.elements.ArrayHashElement;
import com.jetbrains.php.lang.psi.visitors.PhpElementVisitor;
import com.nvlad.yii2support.i18n.TranslationProvider;
import com.nvlad.yii2support.i18n.TranslationProviderUtil;
import com.nvlad.yii2support.i18n.TranslationUsageIndex;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class Yii2NavigatorUnusedTranslationsInspection extends PhpInspection {
    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder problemsHolder, boolean isOnTheFly) {
        return new PhpElementVisitor() {
            @Override
            public void visitFile(PsiFile file) {
                Project project = file.getProject();
                if (DumbService.isDumb(project)) {
                    return;
                }

                TranslationProvider provider = TranslationProviderUtil.resolve(file);
                if (provider == null) {
                    return;
                }

                Set<String> usedMessages = TranslationUsageIndex.getMessages(project, provider.category());
                for (ArrayHashElement element : provider.array().getHashElements()) {
                    PsiElement key = element.getKey();
                    String message = TranslationProviderUtil.resolveStaticMessageKey(key);
                    if (message == null) {
                        continue;
                    }

                    if (!usedMessages.contains(message)) {
                        problemsHolder.registerProblem(
                                key,
                                "Unused translation '" + message + "'",
                                ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                        );
                    }
                }
            }
        };
    }
}
