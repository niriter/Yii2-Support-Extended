package com.nvlad.yii2support.i18n;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.indexing.DataIndexer;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.indexing.FileContent;
import com.intellij.util.indexing.ID;
import com.intellij.util.indexing.ScalarIndexExtension;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import com.jetbrains.php.lang.PhpFileType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Index of categories and statically declared message keys in standard Yii translation providers.
 */
public final class TranslationKeyIndex extends ScalarIndexExtension<String> {
    public static final ID<String, Void> NAME = ID.create("Yii2Support.TranslationKeyIndex");
    private static final String CATEGORY_KEY_PREFIX = "C:";

    @NotNull
    @Override
    public ID<String, Void> getName() {
        return NAME;
    }

    @NotNull
    @Override
    public DataIndexer<String, Void, FileContent> getIndexer() {
        return inputData -> {
            TranslationProvider provider = TranslationProviderUtil.resolve(inputData.getPsiFile());
            if (provider == null) {
                return Collections.emptyMap();
            }

            Map<String, Void> result = new HashMap<>();
            result.put(toCategoryIndexKey(provider.category()), null);
            for (String message : provider.messages()) {
                result.put(toIndexKey(provider.category(), message), null);
            }
            return result;
        };
    }

    @NotNull
    @Override
    public KeyDescriptor<String> getKeyDescriptor() {
        return EnumeratorStringDescriptor.INSTANCE;
    }

    @Override
    public int getVersion() {
        return TranslationIndexVersions.KEYS;
    }

    @NotNull
    @Override
    public FileBasedIndex.InputFilter getInputFilter() {
        return file -> file.getFileType() == PhpFileType.INSTANCE
                && TranslationProviderUtil.isProviderPath(file.getPath());
    }

    @Override
    public boolean dependsOnFileContent() {
        return true;
    }

    public static boolean contains(
            @NotNull Project project,
            @NotNull String category,
            @NotNull String message
    ) {
        return !FileBasedIndex.getInstance()
                .getContainingFiles(NAME, toIndexKey(category, message), GlobalSearchScope.projectScope(project))
                .isEmpty();
    }

    @NotNull
    static List<String> getCategories(@NotNull Project project) {
        List<String> categories = new ArrayList<>();
        for (String key : FileBasedIndex.getInstance().getAllKeys(NAME, project)) {
            if (key.startsWith(CATEGORY_KEY_PREFIX)) {
                categories.add(key.substring(CATEGORY_KEY_PREFIX.length()));
            }
        }
        Collections.sort(categories);
        return Collections.unmodifiableList(categories);
    }

    @NotNull
    static Collection<VirtualFile> getProviderFiles(
            @NotNull Project project,
            @NotNull String category
    ) {
        return FileBasedIndex.getInstance().getContainingFiles(
                NAME,
                toCategoryIndexKey(category),
                GlobalSearchScope.projectScope(project)
        );
    }

    @NotNull
    private static String toIndexKey(@NotNull String category, @NotNull String message) {
        return category.length() + ":" + category + message;
    }

    @NotNull
    private static String toCategoryIndexKey(@NotNull String category) {
        return CATEGORY_KEY_PREFIX + category;
    }
}
