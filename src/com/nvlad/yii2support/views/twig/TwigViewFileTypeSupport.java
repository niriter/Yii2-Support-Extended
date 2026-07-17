package com.nvlad.yii2support.views.twig;

import com.intellij.openapi.fileTypes.FileType;
import com.nvlad.yii2support.views.filetypes.ViewFileTypeSupport;
import org.jetbrains.annotations.NotNull;

public final class TwigViewFileTypeSupport implements ViewFileTypeSupport {
    private static final String TWIG_FILE_TYPE_NAME = "Twig";
    private static final String TWIG_VIEW_TEMPLATE_NAME = "Yii2 Twig View File";

    @Override
    public boolean supports(@NotNull FileType fileType) {
        return TWIG_FILE_TYPE_NAME.equals(fileType.getName());
    }

    @NotNull
    @Override
    public String getTemplateName() {
        return TWIG_VIEW_TEMPLATE_NAME;
    }
}
