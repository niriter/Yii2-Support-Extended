package com.nvlad.yii2support.migrations.services;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileEvent;
import com.intellij.openapi.vfs.VirtualFileListener;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.codeInsight.controlFlow.instructions.PhpClassDeclarationInstruction;
import com.jetbrains.php.codeInsight.controlFlow.instructions.PhpInstruction;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.nvlad.yii2support.common.ClassUtils;
import org.jetbrains.annotations.NotNull;

public final class MigrationsVirtualFileMonitor implements VirtualFileListener {
    private final Project project;
    private final MigrationService service;

    public MigrationsVirtualFileMonitor(Project project) {
        this.project = project;
        service = MigrationService.getInstance(project);
    }

    @Override
    public void fileCreated(@NotNull VirtualFileEvent event) {
        VirtualFile file = event.getFile();
        DumbService.getInstance(project).runWhenSmart(() ->
                ApplicationManager.getApplication().executeOnPooledThread(() -> {
                    if (!project.isDisposed() && ReadAction.compute(() -> isMigrationFile(file))) {
                        service.syncAsync();
                    }
                })
        );
    }

    @Override
    public void fileDeleted(@NotNull VirtualFileEvent event) {
        service.syncAsync();
    }

    private boolean isMigrationFile(VirtualFile virtualFile) {
        if (!virtualFile.isValid()) {
            return false;
        }

        PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
        if (!(psiFile instanceof PhpFile)) {
            return false;
        }

        PhpIndex phpIndex = PhpIndex.getInstance(project);
        for (PhpInstruction instruction : ((PhpFile) psiFile).getControlFlow().getInstructions()) {
            if (instruction instanceof PhpClassDeclarationInstruction) {
                PhpClass phpClass = ((PhpClassDeclarationInstruction) instruction).getClassDeclaration();
                if (!phpClass.isAbstract()
                        && ClassUtils.isClassInheritsOrEqual(phpClass, "\\yii\\db\\Migration", phpIndex)) {
                    return true;
                }
            }
        }
        return false;
    }
}
