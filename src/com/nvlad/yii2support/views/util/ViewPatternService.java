package com.nvlad.yii2support.views.util;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.nvlad.yii2support.utils.Yii2SupportSettings;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Service(Service.Level.PROJECT)
public final class ViewPatternService {
    private final Project project;
    private volatile Map<Pattern, String> patterns;

    public ViewPatternService(Project project) {
        this.project = project;
    }

    @NotNull
    public Map<Pattern, String> getPatterns() {
        Map<Pattern, String> result = patterns;
        if (result != null) {
            return result;
        }

        synchronized (this) {
            if (patterns == null) {
                Map<Pattern, String> newPatterns = new LinkedHashMap<>();
                for (Map.Entry<String, String> entry : Yii2SupportSettings.getInstance(project).viewPathMap.entrySet()) {
                    String patternString = "^(" + entry.getKey().replace("*", "([\\w-]+)") + ").+";
                    newPatterns.put(Pattern.compile(patternString), entry.getValue());
                }
                patterns = Collections.unmodifiableMap(newPatterns);
            }
            return patterns;
        }
    }

    public synchronized void reset() {
        patterns = null;
    }
}
