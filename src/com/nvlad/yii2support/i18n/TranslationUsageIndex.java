package com.nvlad.yii2support.i18n;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.indexing.DataIndexer;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.indexing.FileBasedIndexExtension;
import com.intellij.util.indexing.FileContent;
import com.intellij.util.indexing.ID;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Index of non-interpolated literal messages used by PHP Yii::t() calls.
 */
public final class TranslationUsageIndex extends FileBasedIndexExtension<String, Set<String>> {
    public static final ID<String, Set<String>> NAME = ID.create("Yii2Support.TranslationUsageIndex");
    private static final DataExternalizer<Set<String>> VALUE_EXTERNALIZER = new MessageSetExternalizer();

    @NotNull
    @Override
    public ID<String, Set<String>> getName() {
        return NAME;
    }

    @NotNull
    @Override
    public DataIndexer<String, Set<String>, FileContent> getIndexer() {
        return inputData -> {
            PsiFile file = inputData.getPsiFile();
            Map<String, Set<String>> result = new HashMap<>();

            for (MethodReference reference : PsiTreeUtil.findChildrenOfType(file, MethodReference.class)) {
                TranslationCall call = TranslationCallResolver.resolve(reference);
                if (call == null || call.interpolated()) {
                    continue;
                }

                result.computeIfAbsent(call.category(), ignored -> new HashSet<>())
                        .add(call.message());
            }

            return result;
        };
    }

    @NotNull
    @Override
    public KeyDescriptor<String> getKeyDescriptor() {
        return EnumeratorStringDescriptor.INSTANCE;
    }

    @NotNull
    @Override
    public DataExternalizer<Set<String>> getValueExternalizer() {
        return VALUE_EXTERNALIZER;
    }

    @Override
    public int getVersion() {
        return TranslationIndexVersions.USAGES;
    }

    @NotNull
    @Override
    public FileBasedIndex.InputFilter getInputFilter() {
        return file -> file.getFileType() == PhpFileType.INSTANCE;
    }

    @Override
    public boolean dependsOnFileContent() {
        return true;
    }

    @NotNull
    public static Set<String> getMessages(@NotNull Project project, @NotNull String category) {
        Set<String> result = new HashSet<>();
        for (Set<String> messages : FileBasedIndex.getInstance()
                .getValues(NAME, category, GlobalSearchScope.projectScope(project))) {
            result.addAll(messages);
        }
        return Collections.unmodifiableSet(result);
    }

    private static final class MessageSetExternalizer implements DataExternalizer<Set<String>> {
        @Override
        public void save(@NotNull DataOutput output, @NotNull Set<String> messages) throws IOException {
            List<String> sortedMessages = new ArrayList<>(messages);
            Collections.sort(sortedMessages);
            output.writeInt(sortedMessages.size());
            for (String message : sortedMessages) {
                byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
                output.writeInt(bytes.length);
                output.write(bytes);
            }
        }

        @NotNull
        @Override
        public Set<String> read(@NotNull DataInput input) throws IOException {
            int size = input.readInt();
            Set<String> messages = new HashSet<>(size);
            for (int i = 0; i < size; i++) {
                int byteCount = input.readInt();
                byte[] bytes = new byte[byteCount];
                input.readFully(bytes);
                messages.add(new String(bytes, StandardCharsets.UTF_8));
            }
            return messages;
        }
    }
}
