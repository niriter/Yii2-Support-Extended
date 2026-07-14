package com.nvlad.yii2support.objectfactory;

import com.jetbrains.php.lang.psi.elements.PhpClass;
import junit.framework.TestCase;

import java.lang.reflect.Proxy;

public class ObjectFactoryUtilsTest extends TestCase {
    public void testOrderByExpressionInterfaceIsNotTreatedAsObjectFactoryTarget() {
        PhpClass expressionInterface = phpClass("\\yii\\db\\ExpressionInterface");

        assertNull(ObjectFactoryUtils.excludeExpressionInterface(expressionInterface));
    }

    public void testRegularObjectFactoryClassIsPreserved() {
        PhpClass component = phpClass("\\app\\components\\ReportComponent");

        assertSame(component, ObjectFactoryUtils.excludeExpressionInterface(component));
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
