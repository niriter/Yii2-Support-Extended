package com.nvlad.yii2support.i18n;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.psi.elements.ArrayHashElement;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.PhpPsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class CompletionProvider extends com.intellij.codeInsight.completion.CompletionProvider<CompletionParameters> {
    @Override
    protected void addCompletions(
            @NotNull CompletionParameters parameters,
            @NotNull ProcessingContext context,
            @NotNull CompletionResultSet result
    ) {
        MethodReference reference = PsiTreeUtil.getParentOfType(
                parameters.getPosition(),
                MethodReference.class
        );
        if (reference == null || !TranslationCallResolver.isTranslationMethod(reference)) {
            return;
        }

        Project project = reference.getProject();
        if (DumbService.isDumb(project)) {
            return;
        }

        PsiElement[] methodParameters = reference.getParameters();
        int parameterIndex = findParameterIndex(methodParameters, parameters.getPosition());
        if (parameterIndex == 0) {
            fillCategories(project, result);
            return;
        }

        if (parameterIndex != 1 || !(methodParameters[1] instanceof PhpPsiElement messageElement)) {
            return;
        }

        String category = TranslationCallResolver.resolveStaticCategory(methodParameters[0]);
        if (category != null) {
            fillMessages(project, messageElement, category, result);
        }
    }

    private static int findParameterIndex(PsiElement[] parameters, PsiElement position) {
        for (int i = 0; i < parameters.length; i++) {
            if (PsiTreeUtil.isAncestor(parameters[i], position, false)) {
                return i;
            }
        }
        return -1;
    }

    private static void fillCategories(Project project, CompletionResultSet result) {
        for (String category : TranslationKeyIndex.getCategories(project)) {
            result.addElement(new CategoryLookupElement(category));
        }
    }

    private static void fillMessages(
            Project project,
            PhpPsiElement messageElement,
            String category,
            CompletionResultSet result
    ) {
        List<VirtualFile> providerFiles = new ArrayList<>(
                TranslationKeyIndex.getProviderFiles(project, category)
        );
        providerFiles.sort(Comparator.comparing(VirtualFile::getPath));

        PsiManager psiManager = PsiManager.getInstance(project);
        Set<String> addedMessages = new HashSet<>();
        for (VirtualFile providerFile : providerFiles) {
            PsiFile file = psiManager.findFile(providerFile);
            TranslationProvider provider = file == null ? null : TranslationProviderUtil.resolve(file);
            if (provider == null) {
                continue;
            }

            for (ArrayHashElement message : provider.array().getHashElements()) {
                String key = TranslationProviderUtil.resolveStaticMessageKey(message.getKey());
                if (key != null && addedMessages.add(key)) {
                    result.addElement(new MessageLookupElement(messageElement, message));
                }
            }
        }
    }
}
