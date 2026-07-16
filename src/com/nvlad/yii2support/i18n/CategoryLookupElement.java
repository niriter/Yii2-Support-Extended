package com.nvlad.yii2support.i18n;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.jetbrains.php.lang.PhpFileType;
import org.jetbrains.annotations.NotNull;

class CategoryLookupElement extends LookupElement {
    private final String category;

    CategoryLookupElement(@NotNull String category) {
        this.category = category;
    }

    @NotNull
    @Override
    public String getLookupString() {
        return category;
    }

    @Override
    public void renderElement(@NotNull LookupElementPresentation presentation) {
        super.renderElement(presentation);
        presentation.setIcon(PhpFileType.INSTANCE.getIcon());
        presentation.setItemText(category);
    }
}
