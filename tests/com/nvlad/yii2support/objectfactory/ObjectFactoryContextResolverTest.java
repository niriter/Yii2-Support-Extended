package com.nvlad.yii2support.objectfactory;

import com.jetbrains.php.lang.psi.elements.PhpClass;
import junit.framework.TestCase;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;

public class ObjectFactoryContextResolverTest extends TestCase {
    private PhpClass component;

    @Override
    protected void setUp() {
        component = phpClass("\\app\\components\\ReportComponent");
    }

    public void testExplicitClassKeyIsConfirmed() {
        assertConfirmed(ObjectFactoryContext.Source.EXPLICIT_CLASS_KEY, component);
    }

    public void testYiiCreateObjectSecondArgumentIsConfirmed() {
        assertConfirmed(ObjectFactoryContext.Source.YII_CREATE_OBJECT, component);
    }

    public void testBaseObjectConstructorConfigIsConfirmed() {
        assertConfirmed(ObjectFactoryContext.Source.BASE_OBJECT_CONSTRUCTOR, component);
    }

    public void testWidgetConfigurationIsConfirmed() {
        assertConfirmed(ObjectFactoryContext.Source.WIDGET_CONFIGURATION, component);
    }

    public void testGridColumnConfigurationIsConfirmed() {
        assertConfirmed(ObjectFactoryContext.Source.GRID_COLUMN, component);
    }

    public void testApplicationConfigComponentIsConfirmed() {
        assertConfirmed(ObjectFactoryContext.Source.APPLICATION_CONFIG_COMPONENT, component);
    }

    public void testTypedWritableNestedPropertyIsConfirmedOnlyForUniqueConfigurableType() {
        ObjectFactoryContext confirmed = ObjectFactoryContextResolver.decide(
                ObjectFactoryContext.Source.NESTED_WRITABLE_PROPERTY,
                component,
                true,
                true
        );
        assertTrue(confirmed.isConfirmedObjectConfiguration());
        assertSame(component, confirmed.getTargetClass());

        ObjectFactoryContext union = ObjectFactoryContextResolver.decide(
                ObjectFactoryContext.Source.NESTED_WRITABLE_PROPERTY,
                component,
                false,
                true
        );
        assertFalse(union.isConfirmedObjectConfiguration());
        assertNull(union.getTargetClass());

        ObjectFactoryContext dto = ObjectFactoryContextResolver.decide(
                ObjectFactoryContext.Source.NESTED_WRITABLE_PROPERTY,
                component,
                true,
                false
        );
        assertFalse(dto.isConfirmedObjectConfiguration());
        assertNull(dto.getTargetClass());
    }

    public void testYiiSetterIsConfirmedOnlyForUniqueConfigurableType() {
        ObjectFactoryContext confirmed = ObjectFactoryContextResolver.decide(
                ObjectFactoryContext.Source.YII_SETTER,
                component,
                true,
                true
        );
        assertTrue(confirmed.isConfirmedObjectConfiguration());
        assertSame(component, confirmed.getTargetClass());

        ObjectFactoryContext ambiguous = ObjectFactoryContextResolver.decide(
                ObjectFactoryContext.Source.YII_SETTER,
                component,
                false,
                true
        );
        assertFalse(ambiguous.isConfirmedObjectConfiguration());
        assertNull(ambiguous.getTargetClass());

        ObjectFactoryContext dto = ObjectFactoryContextResolver.decide(
                ObjectFactoryContext.Source.YII_SETTER,
                component,
                true,
                false
        );
        assertFalse(dto.isConfirmedObjectConfiguration());
        assertNull(dto.getTargetClass());
    }

    public void testArbitraryObjectTypedMethodParameterIsRejected() {
        ObjectFactoryContext context = ObjectFactoryContextResolver.decide(
                ObjectFactoryContext.Source.METHOD_PARAMETER_TYPE,
                component
        );

        assertFalse(context.isConfirmedObjectConfiguration());
        assertEquals(ObjectFactoryContext.Source.METHOD_PARAMETER_TYPE, context.getSource());
        assertSame(component, context.getCandidateClass());
        assertNull(context.getTargetClass());
    }

    public void testUnionTypedMethodParameterIsRejected() {
        ObjectFactoryContext context = ObjectFactoryContextResolver.decide(
                ObjectFactoryContext.Source.METHOD_PARAMETER_TYPE,
                component,
                false,
                false
        );

        assertFalse(context.isConfirmedObjectConfiguration());
        assertNull(context.getTargetClass());
    }

    public void testExpressionInterfaceRemainsRejectedForEveryPositiveSource() {
        PhpClass expressionInterface = phpClass("\\yii\\db\\ExpressionInterface");

        for (ObjectFactoryContext.Source source : ObjectFactoryContext.Source.values()) {
            if (source == ObjectFactoryContext.Source.NONE
                    || source == ObjectFactoryContext.Source.METHOD_PARAMETER_TYPE) {
                continue;
            }

            ObjectFactoryContext context = ObjectFactoryContextResolver.decide(source, expressionInterface);
            assertFalse(source.name(), context.isConfirmedObjectConfiguration());
            assertNull(source.name(), context.getTargetClass());
        }
    }

    public void testUnknownAssociativeArrayHasNoneContextAndNullTarget() {
        ObjectFactoryContext context = ObjectFactoryUtils.resolveContext(null, null);

        assertEquals(ObjectFactoryContext.Source.NONE, context.getSource());
        assertFalse(context.isConfirmedObjectConfiguration());
        assertNull(context.getCandidateClass());
        assertNull(context.getTargetClass());
    }

    public void testAllObjectFactoryConsumersUseContextDecision() throws IOException {
        assertConsumerUsesContext("ObjectFactoryMissedFieldInspection$1");
        assertConsumerUsesContext("ObjectFactoryCompletionProvider");
        assertConsumerUsesContext("ObjectFactoryReferenceContributor");
    }

    private void assertConfirmed(ObjectFactoryContext.Source source, PhpClass targetClass) {
        ObjectFactoryContext context = ObjectFactoryContextResolver.decide(source, targetClass);

        assertTrue(context.isConfirmedObjectConfiguration());
        assertEquals(source, context.getSource());
        assertSame(targetClass, context.getTargetClass());
    }

    private void assertConsumerUsesContext(String simpleClassName) throws IOException {
        String resourceName = "com/nvlad/yii2support/objectfactory/" + simpleClassName + ".class";
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(resourceName)) {
            assertNotNull(resourceName, stream);
            String classFile = new String(stream.readAllBytes(), StandardCharsets.ISO_8859_1);
            assertTrue(simpleClassName + " must call resolveContext()", classFile.contains("resolveContext"));
            assertFalse(
                    simpleClassName + " must not bypass the context decision",
                    classFile.contains("findClassByArrayCreation")
            );
        }
    }

    private static PhpClass phpClass(String fqn) {
        return (PhpClass) Proxy.newProxyInstance(
                PhpClass.class.getClassLoader(),
                new Class<?>[]{PhpClass.class},
                (proxy, method, arguments) -> {
                    if ("getFQN".equals(method.getName())) {
                        return fqn;
                    }
                    throw new UnsupportedOperationException(method.getName());
                }
        );
    }
}
