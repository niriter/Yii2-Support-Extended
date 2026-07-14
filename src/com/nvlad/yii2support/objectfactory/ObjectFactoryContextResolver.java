package com.nvlad.yii2support.objectfactory;

import com.jetbrains.php.lang.psi.elements.PhpClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Central policy for promoting a PSI-derived class candidate to an Object
 * Factory context. Candidate extraction stays in {@link ObjectFactoryUtils};
 * every consumer receives the decision made here.
 */
final class ObjectFactoryContextResolver {
    private static final String EXPRESSION_INTERFACE_FQN = "\\yii\\db\\ExpressionInterface";

    private ObjectFactoryContextResolver() {
    }

    @NotNull
    static ObjectFactoryContext decide(
            @NotNull ObjectFactoryContext.Source source,
            @Nullable PhpClass candidateClass
    ) {
        return decide(source, candidateClass, true, true);
    }

    @NotNull
    static ObjectFactoryContext decide(
            @NotNull ObjectFactoryContext.Source source,
            @Nullable PhpClass candidateClass,
            boolean unambiguousTarget,
            boolean configurableTarget
    ) {
        if (candidateClass == null) {
            return source == ObjectFactoryContext.Source.NONE
                    ? ObjectFactoryContext.none()
                    : ObjectFactoryContext.rejected(source, null, "context did not resolve a target class");
        }

        if (isExpressionInterface(candidateClass)) {
            return ObjectFactoryContext.rejected(
                    source,
                    candidateClass,
                    "expression/value interfaces are not Object Factory configuration targets"
            );
        }

        if (!unambiguousTarget) {
            return ObjectFactoryContext.rejected(
                    source,
                    candidateClass,
                    "the target type is ambiguous"
            );
        }

        switch (source) {
            case EXPLICIT_CLASS_KEY:
            case YII_CREATE_OBJECT:
            case BASE_OBJECT_CONSTRUCTOR:
            case WIDGET_CONFIGURATION:
            case GRID_COLUMN:
            case APPLICATION_CONFIG_COMPONENT:
                return ObjectFactoryContext.confirmed(source, candidateClass);

            case NESTED_WRITABLE_PROPERTY:
                return configurableTarget
                        ? ObjectFactoryContext.confirmed(source, candidateClass)
                        : ObjectFactoryContext.rejected(
                                source,
                                candidateClass,
                                "the writable property type is not a configurable Yii object"
                        );

            case METHOD_PARAMETER_TYPE:
                return ObjectFactoryContext.rejected(
                        source,
                        candidateClass,
                        "an object-typed method parameter is not an Object Factory signal"
                );

            case NONE:
            default:
                return ObjectFactoryContext.none();
        }
    }

    @Nullable
    static PhpClass excludeExpressionInterface(@Nullable PhpClass phpClass) {
        return phpClass != null && isExpressionInterface(phpClass) ? null : phpClass;
    }

    private static boolean isExpressionInterface(@NotNull PhpClass phpClass) {
        return EXPRESSION_INTERFACE_FQN.equals(phpClass.getFQN());
    }
}
