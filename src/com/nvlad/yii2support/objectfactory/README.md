Code completion of keys in arrays that have "class" key and valid class reference in value.
Following class reference representations supported:
 - String represention, 
 - Class::class
 - Class::className().
 
 Code completion also works for standard classes (detected by key) in config directory. 
 
 widget() and begin() method for \yii\base\Widget class descendants is supported.
 
 widget() method for \yii\widgets\ActiveField is supported 
  
 Code completion for GridView columns is supported
 
 This module supports Go To Declaration, Rename, Find usages
 
 ------------------------------------
 Configuration arrays
 
 Code completion for Yii configuration arrays. Works both in configuration files and on object instantiation.
 Following cases are supported:
 * Array in $config parameter in yii\base\Object or its descendants constructor 
 * Array have "class" key with valid class representation: fully qualified string representation, ClassName::class or Class::className()
 * Array is a value of a key that corresponds to standard Yii classes (like "db", "request", "mailer" and so on), and file with this array located in a directory called "config"
 * WidgetClass::widget() and WidgetClass::begin calls if WidgetClass is a descendant of yii\base\Widget
 * $field->widget() method call on yii\widgets\ActiveField and its descendants
 * Inside array in GridView "columns" key
 * Nested array assigned to an unambiguous writable property whose type is a yii\base\BaseObject descendant
 
Object Factory support is context-aware. A generic object or union type on an arbitrary method parameter is not enough to activate completion, inspection, or references. Ordinary associative arrays, SQL DSL arrays, DTO/map data, and ambiguous nested property types are ignored unless one of the positive Yii configuration contexts above is present.
 
Go To Declaration, Rename, Find usages and Help popup works whenever code completion works
