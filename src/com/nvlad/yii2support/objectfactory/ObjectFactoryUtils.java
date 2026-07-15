package com.nvlad.yii2support.objectfactory;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.*;
import com.nvlad.yii2support.common.ClassUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by oleg on 14.03.2017.
 */
public class ObjectFactoryUtils {
    @Nullable
    static public PhpClass findClassByArray(@NotNull ArrayCreationExpression arrayCreationExpression) {
        for (ArrayHashElement arrayHashElement : arrayCreationExpression.getHashElements()) {
            PhpPsiElement child = arrayHashElement.getKey();
            if (child instanceof StringLiteralExpression) {
                String key = ((StringLiteralExpression) child).getContents();
                if (key.equals("class") || key.equals("__class")) {
                    Project project = child.getProject();
                    PhpPsiElement value = arrayHashElement.getValue();
                    PhpClass methodRef = ClassUtils.getPhpClassUniversal(project, value);
                    if (methodRef != null) {
                        return methodRef;
                    }
                }
            }
        }

        return null;
    }

    @Nullable
    private static PhpClass getPhpClassByYiiCreateObject(ArrayCreationExpression arrayCreation) {
        PhpClass phpClass = null;
        PsiElement parent = getParent(arrayCreation, 2);
        if (parent instanceof MethodReference) {
            MethodReference method = (MethodReference) parent;
            if (method.getName() != null && method.getName().equals("createObject")) {
                PhpExpression methodClass = method.getClassReference();
                if (isYiiFacade(methodClass)) {
                    PsiElement[] pList = method.getParameters();
                    if (pList.length == 2
                            && pList[0] instanceof PhpPsiElement
                            && ClassUtils.indexForElementInParameterList(arrayCreation) == 1) { // \Yii::createObject takes 2 parameters
                        phpClass = ClassUtils.getPhpClassUniversal(method.getProject(), (PhpPsiElement) pList[0]);
                    }
                }
            }
        }
        return phpClass;
    }

    private static boolean isYiiFacade(@Nullable PhpExpression expression) {
        if (!(expression instanceof ClassReference)) {
            return false;
        }

        ClassReference reference = (ClassReference) expression;
        if ("\\Yii".equals(reference.getFQN())) {
            return true;
        }

        PsiElement resolved = reference.resolve();
        return resolved instanceof PhpClass && "\\Yii".equals(((PhpClass) resolved).getFQN());
    }

    private static PhpClass getPhpClassInConfig(PsiDirectory dir, ArrayCreationExpression arrayCreation) {
        PhpClass phpClass = null;
        if (dir != null && dir.getName().equals("config")) {
            PsiElement parent = getParent(arrayCreation, 2);
            if (parent instanceof ArrayHashElement) {
                ArrayHashElement hash = (ArrayHashElement) parent;
                PsiElement element = hash.getKey();
                if (element instanceof StringLiteralExpression) {
                    StringLiteralExpression literal = (StringLiteralExpression) element;
                    String key = literal.getContents();
                    phpClass = getStandardPhpClass(PhpIndex.getInstance(literal.getProject()), key);
                }
            }
        }
        return phpClass;
    }

    private static PhpClass getPhpClassInWidget(ArrayCreationExpression arrayCreation) {
        PsiElement parent = getParent(arrayCreation, 2);
        if (parent instanceof MethodReference) {
            MethodReference methodRef = (MethodReference) parent;
            if (methodRef.getName() != null && (methodRef.getName().equals("widget") || methodRef.getName().equals("begin"))) {
                PsiElement resolvedMethod = methodRef.resolve();
                Method method = resolvedMethod instanceof Method ? (Method) resolvedMethod : null;
                PhpExpression ref = methodRef.getClassReference();
                if (ref instanceof ClassReference && ClassUtils.indexForElementInParameterList(arrayCreation) == 0) {
                    PsiElement resolvedClass = ((ClassReference) ref).resolve();
                    PhpClass callingClass = resolvedClass instanceof PhpClass ? (PhpClass) resolvedClass : null;
                    PhpClass superClass = ClassUtils.getClass(PhpIndex.getInstance(methodRef.getProject()), "\\yii\\base\\Widget");
                    if (ClassUtils.isClassInheritsOrEqual(callingClass, superClass, 100)) {
                        return callingClass;
                    }
                } else if (method != null && ref instanceof MethodReference && ClassUtils.indexForElementInParameterList(arrayCreation) == 1) {
                    // This code process
                    // $form->field($model, 'username')->widget(\Class::className())
                    PhpClass callingClass = method.getContainingClass();
                    PhpClass superClass = ClassUtils.getClass(PhpIndex.getInstance(methodRef.getProject()), "yii\\widgets\\ActiveField");
                    if (ClassUtils.isClassInheritsOrEqual(callingClass, superClass, 100)
                            && method.getParameters().length == 2 &&
                            method.getParameters()[0].getName().equals("class")) {
                        PsiElement element = methodRef.getParameters()[0];
                        if (element instanceof PhpPsiElement) {
                            PhpClass widgetClass = ClassUtils.getPhpClassUniversal(methodRef.getProject(), (PhpPsiElement) element);
                            if (widgetClass != null) {
                                return widgetClass;
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    private static PhpClass getPhpClassInGridColumns(ArrayCreationExpression arrayCreation) {
        PsiElement parent = getParent(arrayCreation, 2);
        if (parent instanceof ArrayCreationExpression) {
            PsiElement possibleHashElement = getParent(arrayCreation, 4);
            if (!(possibleHashElement instanceof ArrayHashElement)) {
                return null;
            }

            PsiElement key = ((ArrayHashElement) possibleHashElement).getKey();
            if (key != null &&
                    key.getText() != null &&
                    key.getText().replace("\"", "").replace("\'", "").equals("columns")) {
                PsiElement methodRef = getParent(possibleHashElement, 3);
                if (methodRef instanceof MethodReference) {
                    MethodReference method = (MethodReference) methodRef;
                    if (method.getClassReference() != null) {
                        PhpExpression methodClass = method.getClassReference();
                        if (!(methodClass instanceof ClassReference)) {
                            return null;
                        }

                        PhpIndex phpIndex = PhpIndex.getInstance(methodClass.getProject());
                        PsiElement resolvedClass = ((ClassReference) methodClass).resolve();
                        PhpClass callingClass = resolvedClass instanceof PhpClass ? (PhpClass) resolvedClass : null;
                        if (callingClass != null && ClassUtils.isClassInheritsOrEqual(callingClass, "\\yii\\grid\\GridView", phpIndex)) {
                            return ClassUtils.getClass(phpIndex, "\\yii\\grid\\DataColumn");
                        }
                    }
                }
            }
        }

        return null;
    }

    @NotNull
    static ObjectFactoryContext resolveContext(@Nullable ArrayCreationExpression arrayCreation, @Nullable PsiDirectory dir) {
        if (arrayCreation == null) {
            return ObjectFactoryContext.none();
        }

        ObjectFactoryContext context = decide(
                ObjectFactoryContext.Source.EXPLICIT_CLASS_KEY,
                findClassByArray(arrayCreation)
        );
        if (context.getCandidateClass() != null) {
            return context;
        }

        context = decide(
                ObjectFactoryContext.Source.BASE_OBJECT_CONSTRUCTOR,
                getClassByInstantiation(arrayCreation)
        );
        if (context.getCandidateClass() != null) {
            return context;
        }

        context = decide(
                ObjectFactoryContext.Source.YII_CREATE_OBJECT,
                getPhpClassByYiiCreateObject(arrayCreation)
        );
        if (context.getCandidateClass() != null) {
            return context;
        }

        context = decide(
                ObjectFactoryContext.Source.WIDGET_CONFIGURATION,
                getPhpClassInWidget(arrayCreation)
        );
        if (context.getCandidateClass() != null) {
            return context;
        }

        context = decide(
                ObjectFactoryContext.Source.GRID_COLUMN,
                getPhpClassInGridColumns(arrayCreation)
        );
        if (context.getCandidateClass() != null) {
            return context;
        }

        context = getContextInYiiSetter(arrayCreation);
        if (context.getCandidateClass() != null) {
            return context;
        }

        PsiElement arrayParent = getParent(arrayCreation, 2);
        if (arrayParent instanceof ArrayHashElement) {
            context = getContextByHash((ArrayHashElement) arrayParent, dir);
            if (context.getCandidateClass() != null) {
                return context;
            }
        }

        context = decide(
                ObjectFactoryContext.Source.APPLICATION_CONFIG_COMPONENT,
                getPhpClassInConfig(dir, arrayCreation)
        );
        if (context.getCandidateClass() != null) {
            return context;
        }

        // A parameter type is retained only as diagnostic evidence. The policy
        // deliberately rejects it because an array argument is commonly a DSL,
        // map, list or DTO even when the parameter also has an object type.
        PhpClass parameterType = getClassByParameterType(arrayCreation);
        if (parameterType != null) {
            return decide(ObjectFactoryContext.Source.METHOD_PARAMETER_TYPE, parameterType);
        }

        return ObjectFactoryContext.none();
    }

    @NotNull
    private static ObjectFactoryContext decide(
            @NotNull ObjectFactoryContext.Source source,
            @Nullable PhpClass candidateClass
    ) {
        return ObjectFactoryContextResolver.decide(source, candidateClass);
    }

    /**
     * Array arguments such as Query::orderBy() can be typed as ExpressionInterface.
     * They are SQL expressions, not Yii object factory configuration arrays.
     */
    @Nullable
    static PhpClass excludeExpressionInterface(@Nullable PhpClass phpClass) {
        return ObjectFactoryContextResolver.excludeExpressionInterface(phpClass);
    }

    /**
     * Collect an unconfirmed class candidate from a method parameter.
     *
     * This information must pass through {@link ObjectFactoryContextResolver};
     * a parameter type alone is deliberately never a positive Object Factory
     * decision.
     *
     * @return Class
     */
    @Nullable
    private static PhpClass getClassByParameterType(ArrayCreationExpression arrayCreation) {
        if (arrayCreation.getParent() instanceof ParameterList) {
            int index = ClassUtils.indexForElementInParameterList(arrayCreation);
            if (index > -1) {
                PsiElement possibleMethodRef = arrayCreation.getParent().getParent();
                if (possibleMethodRef instanceof MethodReference) {
                    PsiElement resolvedMethod = ((MethodReference) possibleMethodRef).resolve();
                    Method method = resolvedMethod instanceof Method ? (Method) resolvedMethod : null;
                    if (method != null && method.getParameters().length > index) {
                        Parameter parameter = method.getParameters()[index];
                        PhpClass resultClass = ClassUtils.getElementType(parameter);
                        if (resultClass != null) {
                            return resultClass;
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * Yii setters are a documented object-configuration context. Unlike a
     * generic method parameter, the method must be a writable-property setter
     * on a BaseObject descendant and its union must identify exactly one
     * configurable object target (for example array|Sort|bool).
     */
    @NotNull
    private static ObjectFactoryContext getContextInYiiSetter(ArrayCreationExpression arrayCreation) {
        if (!(arrayCreation.getParent() instanceof ParameterList)
                || ClassUtils.indexForElementInParameterList(arrayCreation) != 0) {
            return ObjectFactoryContext.none();
        }

        PsiElement possibleMethodReference = getParent(arrayCreation, 2);
        if (!(possibleMethodReference instanceof MethodReference)) {
            return ObjectFactoryContext.none();
        }

        PsiElement resolvedMethod = ((MethodReference) possibleMethodReference).resolve();
        if (!(resolvedMethod instanceof Method)) {
            return ObjectFactoryContext.none();
        }

        Method method = (Method) resolvedMethod;
        String methodName = method.getName();
        Parameter[] parameters = method.getParameters();
        PhpClass containingClass = method.getContainingClass();
        if (methodName.length() <= 3
                || !methodName.startsWith("set")
                || !Character.isUpperCase(methodName.charAt(3))
                || parameters.length != 1
                || method.isStatic()
                || !method.getAccess().isPublic()
                || !isYiiConfigurableObject(containingClass)) {
            return ObjectFactoryContext.none();
        }

        ObjectTypeResolution target = resolveObjectType(parameters[0]);
        return ObjectFactoryContextResolver.decide(
                ObjectFactoryContext.Source.YII_SETTER,
                target.candidateClass,
                target.unambiguous,
                isYiiConfigurableObject(target.candidateClass)
        );
    }

    @NotNull
    private static ObjectTypeResolution resolveObjectType(@NotNull PhpNamedElement element) {
        PhpIndex index = PhpIndex.getInstance(element.getProject());
        Map<String, PhpClass> objectTypes = new LinkedHashMap<>();
        boolean supportedUnion = true;
        boolean hasArrayBranch = false;

        for (String declaredType : element.getType().getTypes()) {
            for (String type : declaredType.split("\\|")) {
                String normalizedType = normalizeType(type);
                if (normalizedType.equals("array")) {
                    hasArrayBranch = true;
                    continue;
                }
                if (isAllowedConfigurationScalar(normalizedType)) {
                    continue;
                }

                PhpClass phpClass = ClassUtils.getClass(index, type.trim());
                if (phpClass == null) {
                    supportedUnion = false;
                    continue;
                }

                objectTypes.put(phpClass.getFQN(), phpClass);
            }
        }

        PhpClass candidateClass = objectTypes.isEmpty()
                ? null
                : objectTypes.values().iterator().next();
        return new ObjectTypeResolution(
                candidateClass,
                supportedUnion && hasArrayBranch && objectTypes.size() == 1
        );
    }

    @NotNull
    private static String normalizeType(@NotNull String type) {
        String normalized = type.trim().toLowerCase();
        while (normalized.startsWith("?") || normalized.startsWith("\\")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    private static boolean isAllowedConfigurationScalar(@NotNull String normalizedType) {
        return normalizedType.equals("bool")
                || normalizedType.equals("boolean")
                || normalizedType.equals("false")
                || normalizedType.equals("true")
                || normalizedType.equals("null");
    }

    private static final class ObjectTypeResolution {
        private final PhpClass candidateClass;
        private final boolean unambiguous;

        private ObjectTypeResolution(@Nullable PhpClass candidateClass, boolean unambiguous) {
            this.candidateClass = candidateClass;
            this.unambiguous = unambiguous;
        }
    }

    @NotNull
    private static ObjectFactoryContext getContextByHash(ArrayHashElement hashElement, @Nullable PsiDirectory dir) {
        if (hashElement.getParent() instanceof ArrayCreationExpression) {
            ObjectFactoryContext parentContext = resolveContext((ArrayCreationExpression) hashElement.getParent(), dir);
            PhpClass parentClass = parentContext.getTargetClass();
            if (parentClass == null) {
                return ObjectFactoryContext.none();
            }

            String fieldName = hashElement.getKey() != null ? hashElement.getKey().getText() : null;
            if (fieldName == null) {
                return ObjectFactoryContext.none();
            }

            PhpClassMember field = ClassUtils.findWritableField(parentClass, fieldName);
            if (field == null) {
                return ObjectFactoryContext.none();
            }

            PhpNamedElement typeElement = getWritableMemberTypeElement(field);
            if (typeElement == null) {
                return ObjectFactoryContext.none();
            }

            Set<String> declaredTypes = typeElement.getType().getTypes();
            PhpClass resultClass = ClassUtils.getElementType(typeElement, false);
            boolean unambiguous = declaredTypes.size() == 1;
            boolean configurable = isYiiConfigurableObject(resultClass);

            return ObjectFactoryContextResolver.decide(
                    ObjectFactoryContext.Source.NESTED_WRITABLE_PROPERTY,
                    resultClass,
                    unambiguous,
                    configurable
            );
        }

        return ObjectFactoryContext.none();
    }

    @Nullable
    private static PhpNamedElement getWritableMemberTypeElement(@NotNull PhpClassMember member) {
        if (member instanceof Method) {
            Parameter[] parameters = ((Method) member).getParameters();
            return parameters.length == 1 ? parameters[0] : null;
        }

        return member;
    }

    private static boolean isYiiConfigurableObject(@Nullable PhpClass phpClass) {
        if (phpClass == null) {
            return false;
        }

        PhpIndex index = PhpIndex.getInstance(phpClass.getProject());
        PhpClass baseObject = ClassUtils.getClass(index, "\\yii\\base\\BaseObject");
        PhpClass legacyObject = ClassUtils.getClass(index, "\\yii\\base\\Object");

        return ClassUtils.isClassInheritsOrEqual(phpClass, baseObject, 100)
                || ClassUtils.isClassInheritsOrEqual(phpClass, legacyObject, 100);
    }

    private static PhpClass getClassByInstantiation(PhpExpression element) {

        PsiElement newElement = getParent(element, 2);
        if (newElement instanceof NewExpression) {
            ClassReference ref = ((NewExpression) newElement).getClassReference();
            if (ref == null) {
                return null;
            }

            PsiElement possiblePhpClass = ref.resolve();
            if(possiblePhpClass instanceof Method){ // Since 2021.1 resolved into Method __construct() of BaseObject class
                possiblePhpClass = ClassUtils.getClass(PhpIndex.getInstance(element.getProject()), ref.getFQN());
            }
            if (!(possiblePhpClass instanceof PhpClass)) {
                return null;
            }

            PhpClass phpClass = (PhpClass) possiblePhpClass;
            Method constructor = phpClass.getConstructor();
            if (constructor == null) {
                return null;
            }

            if (!isYiiConfigurableObject(phpClass)) {
                return null;
            }

            Parameter[] parameterList = constructor.getParameters();
            if (parameterList.length > 0 && parameterList[0].getName().equals("config") && ClassUtils.indexForElementInParameterList(element) == 0) {
                return phpClass;
            }

        }

        return null;
    }

    @Nullable
    private static PsiElement getParent(@NotNull PsiElement element, int levels) {
        PsiElement current = element;
        for (int level = 0; level < levels && current != null; level++) {
            current = current.getParent();
        }
        return current;
    }


    private static PhpClass getStandardPhpClass(PhpIndex phpIndex, String shortName) {
        switch (shortName) {
            // web/Application
            case "request":
                return ClassUtils.getClass(phpIndex, "\\yii\\web\\Request");
            case "response":
                return ClassUtils.getClass(phpIndex, "\\yii\\web\\Response");
            case "session":
                return ClassUtils.getClass(phpIndex, "\\yii\\web\\Session");
            case "user":
                return ClassUtils.getClass(phpIndex, "\\yii\\web\\User");
            case "errorHandler":
                return ClassUtils.getClass(phpIndex, "\\yii\\web\\ErrorHandler");
            // base/Application
            case "log":
                return ClassUtils.getClass(phpIndex, "\\yii\\log\\Dispatcher");
            case "view":
                return ClassUtils.getClass(phpIndex, "\\yii\\web\\View");
            case "formatter":
                return ClassUtils.getClass(phpIndex, "yii\\i18n\\Formatter");
            case "i18n":
                return ClassUtils.getClass(phpIndex, "yii\\i18n\\I18N");
            case "mailer":
                return ClassUtils.getClass(phpIndex, "\\yii\\swiftmailer\\Mailer");
            case "urlManager":
                return ClassUtils.getClass(phpIndex, "\\yii\\web\\UrlManager");
            case "assetManager":
                return ClassUtils.getClass(phpIndex, "\\yii\\web\\AssetManager");
            case "security":
                return ClassUtils.getClass(phpIndex, "\\yii\\base\\Security");
            // custom
            case "db":
                return ClassUtils.getClass(phpIndex, "\\yii\\db\\Connection");
        }
        return null;
    }

    @Nullable
    static ArrayCreationExpression getArrayCreationByVarRef(Variable value) {
        ArrayCreationExpression arrayCreation;
        PsiElement arrayDecl = value.resolve();
        if (arrayDecl != null && arrayDecl.getParent() != null && arrayDecl.getParent().getChildren().length > 1) {
            PsiElement psiElement = arrayDecl.getParent().getLastChild();
            if (psiElement instanceof ArrayCreationExpression)
                arrayCreation = (ArrayCreationExpression) psiElement;
            else
                return null;
        } else
            return null;
        return arrayCreation;
    }

    @Nullable
    static ArrayCreationExpression getArrayCreationByFieldRef(FieldReference value) {
        ArrayCreationExpression arrayCreation = null;
        PsiElement arrayDecl = value.resolve();
        if (arrayDecl != null && arrayDecl.getParent() != null && arrayDecl.getParent().getChildren().length > 1) {
            PsiElement psiElement = arrayDecl.getLastChild();
            if (psiElement instanceof ArrayCreationExpression) {
                arrayCreation = (ArrayCreationExpression) psiElement;
            }
        }

        return arrayCreation;
    }
}
