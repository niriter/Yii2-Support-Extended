package com.nvlad.yii2support.views.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.IPopupChooserBuilder;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.ui.JBColor;
import com.intellij.ui.ListCellRendererWithRightAlignedComponent;
import com.jetbrains.php.PhpIcons;
import com.jetbrains.php.lang.psi.elements.Include;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.nvlad.yii2support.common.FileUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OpenViewCalls extends AnAction {
    private static final Pattern FIRST_ARGUMENT_PATTERN = Pattern.compile("([^,]+),");

    @Override
    public void actionPerformed(AnActionEvent e) {
        // TODO: insert action logic here
        PsiFile psiFile = e.getData(PlatformDataKeys.PSI_FILE);
        if (psiFile == null) {
            return;
        }

        Collection<PsiReference> references = ReferencesSearch.search(psiFile).findAll();
        references.removeIf(psiReference -> psiReference.getElement() instanceof Include);

        if (references.size() == 0) {
            return;
        }

        if (references.size() == 1) {
            ReferenceNavigation.openReference(references.iterator().next());
            return;
        }

        IPopupChooserBuilder<PsiReference> popupBuilder = JBPopupFactory.getInstance()
                .createPopupChooserBuilder(new ArrayList<>(references));
        popupBuilder.setRenderer(new ListCellRendererWithRightAlignedComponent<PsiReference>() {
            @Override
            protected void customize(PsiReference reference) {
                if (reference == null || reference.getElement() == null) {
                    setLeftText("(empty)");
                    return;
                }

                PsiElement psiElement = reference.getElement();
                PsiElement methodElement = PsiTreeUtil.getParentOfType(psiElement, MethodReference.class);
                if (methodElement == null) {
                    setLeftText("(empty)");
                    return;
                }

                Project project = methodElement.getProject();
                VirtualFile virtualFile = FileUtil.getVirtualFile(methodElement.getContainingFile());
                String fileName = ReferenceNavigation.getDisplayPath(project, virtualFile);

                Document document = PsiDocumentManager.getInstance(project).getDocument(methodElement.getContainingFile());
                if (document != null) {
                    fileName += ":" + (document.getLineNumber(psiElement.getTextOffset()) + 1);
                }

                Matcher matcher = FIRST_ARGUMENT_PATTERN.matcher(methodElement.getText());
                setLeftText(matcher.find() ? matcher.group(1) + ", ...) " : methodElement.getText() + " ");
                setRightText("..." + fileName + " ");
                setRightForeground(JBColor.GRAY);
                setIcon(PhpIcons.METHOD);
            }
        });

        JBPopup popup = popupBuilder
                .setTitle("Render this View from")
                .setItemChosenCallback(ReferenceNavigation::openReference)
                .createPopup();

        Project project = e.getProject();
        if (project == null) {
            popup.showInFocusCenter();
        } else {
            popup.showCenteredInCurrentWindow(project);
        }
    }
}
