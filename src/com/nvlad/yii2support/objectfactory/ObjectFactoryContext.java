package com.nvlad.yii2support.objectfactory;

import com.jetbrains.php.lang.psi.elements.PhpClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Result of determining whether a PHP array is a Yii object configuration.
 *
 * Keeping the source and the confirmation flag together prevents consumers from
 * accidentally treating a class inferred from an unrelated PHP type as an
 * Object Factory target.
 */
final class ObjectFactoryContext {
    enum Source {
        NONE,
        EXPLICIT_CLASS_KEY,
        YII_CREATE_OBJECT,
        BASE_OBJECT_CONSTRUCTOR,
        WIDGET_CONFIGURATION,
        GRID_COLUMN,
        APPLICATION_CONFIG_COMPONENT,
        NESTED_WRITABLE_PROPERTY,
        METHOD_PARAMETER_TYPE
    }

    private final Source source;
    private final PhpClass candidateClass;
    private final boolean confirmedObjectConfiguration;
    private final String reason;

    private ObjectFactoryContext(
            @NotNull Source source,
            @Nullable PhpClass candidateClass,
            boolean confirmedObjectConfiguration,
            @NotNull String reason
    ) {
        this.source = source;
        this.candidateClass = candidateClass;
        this.confirmedObjectConfiguration = confirmedObjectConfiguration;
        this.reason = reason;
    }

    @NotNull
    static ObjectFactoryContext confirmed(@NotNull Source source, @NotNull PhpClass targetClass) {
        return new ObjectFactoryContext(source, targetClass, true, "confirmed Yii object configuration");
    }

    @NotNull
    static ObjectFactoryContext rejected(
            @NotNull Source source,
            @Nullable PhpClass candidateClass,
            @NotNull String reason
    ) {
        return new ObjectFactoryContext(source, candidateClass, false, reason);
    }

    @NotNull
    static ObjectFactoryContext none() {
        return rejected(Source.NONE, null, "no positive Yii object-configuration context");
    }

    @NotNull
    Source getSource() {
        return source;
    }

    boolean isConfirmedObjectConfiguration() {
        return confirmedObjectConfiguration;
    }

    /**
     * The only class consumers may use for completion, inspection or references.
     */
    @Nullable
    PhpClass getTargetClass() {
        return confirmedObjectConfiguration ? candidateClass : null;
    }

    @Nullable
    PhpClass getCandidateClass() {
        return candidateClass;
    }

    @NotNull
    String getReason() {
        return reason;
    }
}
