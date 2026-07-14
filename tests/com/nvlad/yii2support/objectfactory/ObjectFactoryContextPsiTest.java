package com.nvlad.yii2support.objectfactory;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixture4TestCase;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.psi.elements.ArrayCreationExpression;
import org.junit.Test;

import java.util.Collection;

public class ObjectFactoryContextPsiTest extends LightPlatformCodeInsightFixture4TestCase {
    @Test
    public void testContextDecisionOnRealPhpPsi() {
        addYiiAndApplicationClasses();

        PsiFile file = myFixture.configureByText(PhpFileType.INSTANCE, """
                <?php
                namespace app;

                $explicit = ['class' => SomeComponent::class, 'explicitMarker' => true];
                $explicitLegacy = ['__class' => SomeComponent::class, 'explicitLegacyMarker' => true];
                \\Yii::createObject(SomeComponent::class, ['createObjectMarker' => true]);
                new SomeComponent(['constructorMarker' => true]);
                ReportWidget::widget(['widgetMarker' => true]);
                ReportWidget::begin(['beginWidgetMarker' => true]);
                ReportGrid::widget(['columns' => [['gridColumnMarker' => true]]]);

                $parent = [
                    'class' => SomeComponent::class,
                    'child' => ['nestedPropertyMarker' => true],
                ];

                $service = new ArbitraryService();
                $service->acceptObject(['objectParameterMarker' => true]);
                $service->acceptUnion(['unionParameterMarker' => true]);

                $query = new Query();
                $query->orderBy(['ag.created_at' => SORT_DESC]);

                $unknown = ['unknownArrayMarker' => true];
                """);

        assertContext(file, "['class' => SomeComponent::class, 'explicitMarker' => true]",
                ObjectFactoryContext.Source.EXPLICIT_CLASS_KEY, "\\app\\SomeComponent");
        assertContext(file, "['__class' => SomeComponent::class, 'explicitLegacyMarker' => true]",
                ObjectFactoryContext.Source.EXPLICIT_CLASS_KEY, "\\app\\SomeComponent");
        assertContext(file, "['createObjectMarker' => true]",
                ObjectFactoryContext.Source.YII_CREATE_OBJECT, "\\app\\SomeComponent");
        assertContext(file, "['constructorMarker' => true]",
                ObjectFactoryContext.Source.BASE_OBJECT_CONSTRUCTOR, "\\app\\SomeComponent");
        assertContext(file, "['widgetMarker' => true]",
                ObjectFactoryContext.Source.WIDGET_CONFIGURATION, "\\app\\ReportWidget");
        assertContext(file, "['beginWidgetMarker' => true]",
                ObjectFactoryContext.Source.WIDGET_CONFIGURATION, "\\app\\ReportWidget");
        assertContext(file, "['gridColumnMarker' => true]",
                ObjectFactoryContext.Source.GRID_COLUMN, "\\yii\\grid\\DataColumn");
        assertContext(file, "['nestedPropertyMarker' => true]",
                ObjectFactoryContext.Source.NESTED_WRITABLE_PROPERTY, "\\app\\ChildComponent");

        assertNoContext(file, "['objectParameterMarker' => true]",
                ObjectFactoryContext.Source.METHOD_PARAMETER_TYPE);
        assertNoContext(file, "['unionParameterMarker' => true]",
                ObjectFactoryContext.Source.METHOD_PARAMETER_TYPE);
        assertNoContext(file, "['ag.created_at' => SORT_DESC]", null);
        assertNoContext(file, "['unknownArrayMarker' => true]", ObjectFactoryContext.Source.NONE);

        PsiFile configFile = myFixture.addFileToProject("config/main.php", """
                <?php
                return ['db' => ['dsn' => 'sqlite::memory:']];
                """);
        assertContext(configFile, "['dsn' => 'sqlite::memory:']",
                ObjectFactoryContext.Source.APPLICATION_CONFIG_COMPONENT, "\\yii\\db\\Connection");
    }

    private void addYiiAndApplicationClasses() {
        myFixture.addFileToProject("yii/BaseYii.php", """
                <?php
                namespace yii;
                class BaseYii {}
                """);
        myFixture.addFileToProject("Yii.php", """
                <?php
                class Yii extends \\yii\\BaseYii {
                    public static function createObject($type, array $params = []) {}
                }
                """);
        myFixture.addFileToProject("yii/base/BaseObject.php", """
                <?php
                namespace yii\\base;
                class BaseObject {
                    public function __construct(array $config = []) {}
                }
                """);
        myFixture.addFileToProject("yii/base/Widget.php", """
                <?php
                namespace yii\\base;
                class Widget extends BaseObject {
                    public static function widget(array $config = []) {}
                    public static function begin(array $config = []) {}
                }
                """);
        myFixture.addFileToProject("yii/db/ExpressionInterface.php", """
                <?php
                namespace yii\\db;
                interface ExpressionInterface {}
                """);
        myFixture.addFileToProject("yii/db/Connection.php", """
                <?php
                namespace yii\\db;
                class Connection extends \\yii\\base\\BaseObject {
                    public string $dsn;
                }
                """);
        myFixture.addFileToProject("yii/grid/GridView.php", """
                <?php
                namespace yii\\grid;
                class GridView extends \\yii\\base\\Widget {}
                """);
        myFixture.addFileToProject("yii/grid/DataColumn.php", """
                <?php
                namespace yii\\grid;
                class DataColumn extends \\yii\\base\\BaseObject {
                    public string $attribute;
                }
                """);
        myFixture.addFileToProject("app/Fixtures.php", """
                <?php
                namespace app;

                class ChildComponent extends \\yii\\base\\BaseObject {
                    public bool $nestedPropertyMarker;
                }

                class SomeComponent extends \\yii\\base\\BaseObject {
                    public ChildComponent $child;
                    public bool $explicitMarker;
                    public bool $constructorMarker;
                }

                class ReportWidget extends \\yii\\base\\Widget {
                    public bool $widgetMarker;
                }

                class ReportGrid extends \\yii\\grid\\GridView {}

                class ArbitraryService {
                    public function acceptObject(SomeComponent $value) {}
                    public function acceptUnion(SomeComponent|ChildComponent $value) {}
                }

                class Query {
                    public function orderBy(\\yii\\db\\ExpressionInterface|array $columns) {}
                }
                """);
    }

    private void assertContext(
            PsiFile file,
            String arrayText,
            ObjectFactoryContext.Source source,
            String targetFqn
    ) {
        ArrayCreationExpression array = findArray(file, arrayText);
        ObjectFactoryContext context = ObjectFactoryUtils.resolveContext(array, file.getContainingDirectory());

        assertTrue(arrayText, context.isConfirmedObjectConfiguration());
        assertEquals(arrayText, source, context.getSource());
        assertNotNull(arrayText, context.getTargetClass());
        assertEquals(arrayText, targetFqn, context.getTargetClass().getFQN());
    }

    private void assertNoContext(
            PsiFile file,
            String arrayText,
            ObjectFactoryContext.Source expectedSource
    ) {
        ArrayCreationExpression array = findArray(file, arrayText);
        ObjectFactoryContext context = ObjectFactoryUtils.resolveContext(array, file.getContainingDirectory());

        assertFalse(arrayText, context.isConfirmedObjectConfiguration());
        assertNull(arrayText, context.getTargetClass());
        if (expectedSource != null) {
            assertEquals(arrayText, expectedSource, context.getSource());
        }
    }

    private ArrayCreationExpression findArray(PsiFile file, String expectedText) {
        Collection<ArrayCreationExpression> arrays = PsiTreeUtil.findChildrenOfType(
                file,
                ArrayCreationExpression.class
        );
        for (ArrayCreationExpression array : arrays) {
            if (StringUtil.equalsIgnoreWhitespaces(expectedText, array.getText())) {
                return array;
            }
        }

        fail("Array not found: " + expectedText);
        return null;
    }
}
