package com.nvlad.yii2support.objectfactory;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.psi.elements.ArrayCreationExpression;

import java.util.Collection;

/**
 * Real PHP PSI extraction coverage. This suite is deliberately pinned to the
 * PhpStorm 2024.1 test runtime because newer bundled runtimes no longer expose
 * a compatible CodeInsight fixture API. Portable context-policy tests remain
 * in the regular test source set and run on every supported IDE version.
 */
@SuppressWarnings("removal")
public class ObjectFactoryContextPsiIntegrationTest extends LightPlatformCodeInsightFixtureTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        addYiiAndApplicationClasses();
    }

    public void testPositiveContextExtraction() {
        PsiFile file = configure("""
                $explicit = ['class' => SomeComponent::class, 'explicitMarker' => true];
                $legacy = ['__class' => SomeComponent::class, 'legacyMarker' => true];
                \\Yii::createObject(SomeComponent::class, ['createObjectMarker' => true]);
                new SomeComponent(['constructorMarker' => true]);
                ReportWidget::widget(['widgetMarker' => true]);
                ReportWidget::begin(['beginMarker' => true]);
                ReportGrid::widget(['columns' => [['gridColumnMarker' => true]]]);
                $parent = [
                    'class' => SomeComponent::class,
                    'child' => ['nestedPropertyMarker' => true],
                ];
                """);

        assertContext(file, "['class' => SomeComponent::class, 'explicitMarker' => true]",
                ObjectFactoryContext.Source.EXPLICIT_CLASS_KEY, "\\app\\SomeComponent");
        assertContext(file, "['__class' => SomeComponent::class, 'legacyMarker' => true]",
                ObjectFactoryContext.Source.EXPLICIT_CLASS_KEY, "\\app\\SomeComponent");
        assertContext(file, "['createObjectMarker' => true]",
                ObjectFactoryContext.Source.YII_CREATE_OBJECT, "\\app\\SomeComponent");
        assertContext(file, "['constructorMarker' => true]",
                ObjectFactoryContext.Source.BASE_OBJECT_CONSTRUCTOR, "\\app\\SomeComponent");
        assertContext(file, "['widgetMarker' => true]",
                ObjectFactoryContext.Source.WIDGET_CONFIGURATION, "\\app\\ReportWidget");
        assertContext(file, "['beginMarker' => true]",
                ObjectFactoryContext.Source.WIDGET_CONFIGURATION, "\\app\\ReportWidget");
        assertContext(file, "['gridColumnMarker' => true]",
                ObjectFactoryContext.Source.GRID_COLUMN, "\\yii\\grid\\DataColumn");
        assertContext(file, "['nestedPropertyMarker' => true]",
                ObjectFactoryContext.Source.NESTED_WRITABLE_PROPERTY, "\\app\\ChildComponent");
    }

    public void testYiiSetterContextExtraction() {
        PsiFile file = configure("""
                $provider = new \\yii\\data\\ActiveDataProvider();
                $provider->setSort([
                    'defaultOrder' => ['created_at' => SORT_DESC],
                ]);
                $provider->setPagination(['pageSize' => 20]);

                $ambiguous = new AmbiguousProvider();
                $ambiguous->setDisplay(['ambiguousSetterMarker' => true]);
                $ambiguous->setDto(['dtoSetterMarker' => true]);
                $ambiguous->setStrict(['strictSetterMarker' => true]);
                $ambiguous->setStringy(['stringSetterMarker' => true]);

                $service = new ArbitraryService();
                $service->setOptions(['nonYiiSetterMarker' => true]);
                """);

        assertContext(file, "['defaultOrder' => ['created_at' => SORT_DESC],]",
                ObjectFactoryContext.Source.YII_SETTER, "\\yii\\data\\Sort");
        assertContext(file, "['pageSize' => 20]",
                ObjectFactoryContext.Source.YII_SETTER, "\\yii\\data\\Pagination");
        assertNoContext(file, "['ambiguousSetterMarker' => true]",
                ObjectFactoryContext.Source.YII_SETTER);
        assertNoContext(file, "['dtoSetterMarker' => true]",
                ObjectFactoryContext.Source.YII_SETTER);
        assertNoContext(file, "['strictSetterMarker' => true]",
                ObjectFactoryContext.Source.YII_SETTER);
        assertNoContext(file, "['stringSetterMarker' => true]",
                ObjectFactoryContext.Source.YII_SETTER);
        assertNoContext(file, "['nonYiiSetterMarker' => true]", null);
    }

    public void testNegativeContextExtractionUsesActualArrays() {
        PsiFile file = configure("""
                $service = new ArbitraryService();
                $service->acceptObject(['objectParameterMarker' => true]);
                $service->acceptUnion(['unionParameterMarker' => true]);

                $query = new Query();
                $query->orderBy(['ag.created_at' => SORT_DESC]);

                $unknown = ['unknownArrayMarker' => true];
                """);

        assertNoContext(file, "['objectParameterMarker' => true]",
                ObjectFactoryContext.Source.METHOD_PARAMETER_TYPE);
        assertNoContext(file, "['unionParameterMarker' => true]",
                ObjectFactoryContext.Source.METHOD_PARAMETER_TYPE);
        assertNoContext(file, "['ag.created_at' => SORT_DESC]", null);
        assertNoContext(file, "['unknownArrayMarker' => true]",
                ObjectFactoryContext.Source.NONE);
    }

    public void testApplicationConfigExtraction() {
        PsiFile configFile = myFixture.addFileToProject("config/main.php", """
                <?php
                return ['db' => ['dsn' => 'sqlite::memory:']];
                """);

        assertContext(configFile, "['dsn' => 'sqlite::memory:']",
                ObjectFactoryContext.Source.APPLICATION_CONFIG_COMPONENT, "\\yii\\db\\Connection");
    }

    private PsiFile configure(String body) {
        return myFixture.configureByText(
                PhpFileType.INSTANCE,
                "<?php\nnamespace app;\n" + body
        );
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
                class Component extends BaseObject {}
                """);
        myFixture.addFileToProject("yii/base/Widget.php", """
                <?php
                namespace yii\\base;
                class Widget extends Component {
                    public static function widget(array $config = []) {}
                    public static function begin(array $config = []) {}
                }
                """);
        myFixture.addFileToProject("yii/data/DataProvider.php", """
                <?php
                namespace yii\\data;

                class Sort extends \\yii\\base\\Component {
                    public bool $sortMarker;
                    public array $defaultOrder;
                }

                class Pagination extends \\yii\\base\\Component {
                    public bool $paginationMarker;
                    public int $pageSize;
                }

                abstract class BaseDataProvider extends \\yii\\base\\Component {
                    /** @param array|Sort|false $value */
                    public function setSort($value) {}

                    /** @param array|Pagination|false $value */
                    public function setPagination($value) {}
                }

                class ActiveDataProvider extends BaseDataProvider {}
                """);
        myFixture.addFileToProject("yii/db/Database.php", """
                <?php
                namespace yii\\db;
                interface ExpressionInterface {}
                class Connection extends \\yii\\base\\Component {
                    public string $dsn;
                }
                """);
        myFixture.addFileToProject("yii/grid/Grid.php", """
                <?php
                namespace yii\\grid;
                class GridView extends \\yii\\base\\Widget {}
                class DataColumn extends \\yii\\base\\Component {
                    public bool $gridColumnMarker;
                }
                """);
        myFixture.addFileToProject("app/Fixtures.php", """
                <?php
                namespace app;

                class ChildComponent extends \\yii\\base\\Component {
                    public bool $nestedPropertyMarker;
                }

                class SomeComponent extends \\yii\\base\\Component {
                    public ChildComponent $child;
                    public bool $explicitMarker;
                    public bool $legacyMarker;
                    public bool $constructorMarker;
                }

                class ReportWidget extends \\yii\\base\\Widget {
                    public bool $widgetMarker;
                    public bool $beginMarker;
                }

                class ReportGrid extends \\yii\\grid\\GridView {}
                class Dto {}

                class AmbiguousProvider extends \\yii\\base\\Component {
                    /** @param array|\\yii\\data\\Sort|\\yii\\data\\Pagination $value */
                    public function setDisplay($value) {}

                    /** @param array|Dto $value */
                    public function setDto($value) {}

                    public function setStrict(SomeComponent $value) {}

                    /** @param array|\\yii\\data\\Sort|string $value */
                    public function setStringy($value) {}
                }

                class ArbitraryService {
                    public function acceptObject(SomeComponent $value) {}
                    public function acceptUnion(SomeComponent|ChildComponent $value) {}

                    /** @param array|SomeComponent $value */
                    public function setOptions($value) {}
                }

                class Query {
                    /** @param array|\\yii\\db\\ExpressionInterface $columns */
                    public function orderBy($columns) {}
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
        String decision = arrayText
                + ": source=" + context.getSource()
                + ", reason=" + context.getReason()
                + ", candidate=" + (context.getCandidateClass() == null
                ? "null"
                : context.getCandidateClass().getFQN());

        assertTrue(decision, context.isConfirmedObjectConfiguration());
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
