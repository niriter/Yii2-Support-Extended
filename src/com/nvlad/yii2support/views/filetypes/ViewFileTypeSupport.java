package com.nvlad.yii2support.views.filetypes;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.fileTypes.FileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ViewFileTypeSupport {
    ExtensionPointName<ViewFileTypeSupport> EP_NAME = ExtensionPointName.create(
            "com.meekitak.yii2navigator.viewFileTypeSupport"
    );

    boolean supports(@NotNull FileType fileType);

    @NotNull
    String getTemplateName();

    static boolean isSupported(@NotNull FileType fileType) {
        return findSupport(fileType) != null;
    }

    @Nullable
    static String findTemplateName(@NotNull FileType fileType) {
        ViewFileTypeSupport support = findSupport(fileType);
        return support == null ? null : support.getTemplateName();
    }

    @Nullable
    private static ViewFileTypeSupport findSupport(@NotNull FileType fileType) {
        for (ViewFileTypeSupport support : EP_NAME.getExtensionList()) {
            if (support.supports(fileType)) {
                return support;
            }
        }
        return null;
    }
}
