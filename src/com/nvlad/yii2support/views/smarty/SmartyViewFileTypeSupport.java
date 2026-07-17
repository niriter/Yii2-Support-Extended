package com.nvlad.yii2support.views.smarty;

import com.intellij.openapi.fileTypes.FileType;
import com.nvlad.yii2support.views.filetypes.ViewFileTypeSupport;
import org.jetbrains.annotations.NotNull;

public final class SmartyViewFileTypeSupport implements ViewFileTypeSupport {
    private static final String SMARTY_FILE_TYPE_NAME = "Smarty";
    private static final String SMARTY_VIEW_TEMPLATE_NAME = "Yii2 Smarty View File";

    @Override
    public boolean supports(@NotNull FileType fileType) {
        return SMARTY_FILE_TYPE_NAME.equals(fileType.getName());
    }

    @NotNull
    @Override
    public String getTemplateName() {
        return SMARTY_VIEW_TEMPLATE_NAME;
    }
}
