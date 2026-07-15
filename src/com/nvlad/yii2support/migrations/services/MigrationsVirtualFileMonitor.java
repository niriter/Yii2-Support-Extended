package com.nvlad.yii2support.migrations.services;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.codeInsight.controlFlow.instructions.PhpClassDeclarationInstruction;
import com.jetbrains.php.codeInsight.controlFlow.instructions.PhpInstruction;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.nvlad.yii2support.common.ClassUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class MigrationsVirtualFileMonitor implements BulkFileListener {
    private final Project project;
    private final MigrationService service;

    public MigrationsVirtualFileMonitor(Project project) {
        this.project = project;
        service = MigrationService.getInstance(project);
    }

    @Override
    public void after(@NotNull List<? extends VFileEvent> events) {
        List<VirtualFile> createdFiles = new ArrayList<>();
        boolean fileDeleted = false;
        for (VFileEvent event : events) {
            if (event instanceof VFileDeleteEvent) {
                fileDeleted = true;
            } else if (event instanceof VFileCreateEvent && event.getFile() != null) {
                createdFiles.add(event.getFile());
            }
        }

        if (fileDeleted) {
            service.syncAsync();
        }
        if (!createdFiles.isEmpty()) {
            checkCreatedFiles(createdFiles);
        }
    }

    private void checkCreatedFiles(List<VirtualFile> files) {
        DumbService.getInstance(project).runWhenSmart(() ->
                ApplicationManager.getApplication().executeOnPooledThread(() -> {
                    if (project.isDisposed()) {
                        return;
                    }

                    boolean migrationCreated = ApplicationManager.getApplication().runReadAction(
                            (Computable<Boolean>) () -> files.stream().anyMatch(this::isMigrationFile)
                    );
                    if (migrationCreated) {
                        service.syncAsync();
                    }
                })
        );
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
