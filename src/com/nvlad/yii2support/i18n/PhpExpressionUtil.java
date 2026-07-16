package com.nvlad.yii2support.i18n;

import com.jetbrains.php.lang.psi.elements.AssignmentExpression;
import com.jetbrains.php.lang.psi.elements.ClassConstantReference;
import com.jetbrains.php.lang.psi.elements.ClassReference;
import com.jetbrains.php.lang.psi.elements.ConcatenationExpression;
import com.jetbrains.php.lang.psi.elements.Constant;
import com.jetbrains.php.lang.psi.elements.ConstantReference;
import com.jetbrains.php.lang.psi.elements.Field;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpExpression;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import com.jetbrains.php.lang.psi.elements.Variable;
import org.jetbrains.annotations.NotNull;

final class PhpExpressionUtil {
    private PhpExpressionUtil() {
    }

    @NotNull
    static String getValue(@NotNull PhpExpression expression) {
        if (expression instanceof StringLiteralExpression literal) {
            return literal.getContents();
        }
        if (expression instanceof ConstantReference reference
                && reference.resolve() instanceof Constant constant
                && constant.getValue() instanceof PhpExpression value) {
            return getValue(value);
        }
        if (expression instanceof ClassConstantReference constantReference
                && constantReference.getClassReference() instanceof ClassReference classReference
                && classReference.resolve() instanceof PhpClass phpClass) {
            Field field = phpClass.findFieldByName(expression.getName(), true);
            if (field != null && field.getDefaultValue() instanceof PhpExpression value) {
                return getValue(value);
            }
        }
        if (expression instanceof Variable variable
                && variable.resolve() instanceof PhpExpression resolved
                && resolved.getContext() instanceof AssignmentExpression assignment
                && assignment.getValue() instanceof PhpExpression value) {
            return getValue(value);
        }
        if (expression instanceof ConcatenationExpression concatenation
                && concatenation.getLeftOperand() instanceof PhpExpression left
                && concatenation.getRightOperand() instanceof PhpExpression right) {
            return getValue(left) + getValue(right);
        }

        String expressionType = expression.getType().toString();
        if (expressionType.equals("int") || expressionType.equals("float")) {
            return expression.getText();
        }
        return "";
    }
}
