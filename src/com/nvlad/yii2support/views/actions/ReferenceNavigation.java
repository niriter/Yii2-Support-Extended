package com.nvlad.yii2support.views.actions;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;

final class ReferenceNavigation {
    private ReferenceNavigation() {
    }

    static String getDisplayPath(Project project, VirtualFile file) {
        VirtualFile projectRoot = ProjectUtil.guessProjectDir(project);
        String relativePath = projectRoot == null ? null : VfsUtilCore.getRelativePath(file, projectRoot, '/');
        return relativePath == null ? file.getPresentableUrl() : "/" + relativePath;
    }

    static void openReference(PsiReference reference) {
        PsiElement psiElement = reference.getElement();
        if (psiElement.getFirstChild() != null && psiElement.getFirstChild().getNextSibling() != null) {
            psiElement = psiElement.getFirstChild().getNextSibling();
        }

        if (psiElement instanceof Navigatable && ((Navigatable) psiElement).canNavigate()) {
            ((Navigatable) psiElement).navigate(true);
        }
    }
}
