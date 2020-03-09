# Unit Test Resources

This directory contains all the resources that needed for unit tests.

## strings_zh.pack

This file is generated from `res/values-zh/strings.xml` with following content.

```xml
<?xml version="1.0" encoding="utf-8"?>

<resources>
    <string name="hello_world">你好，世界</string>
    <plurals name="plural_list">
        <item quantity="zero">零个</item>
        <item quantity="one">一个</item>
        <item quantity="two">两个</item>
        <item quantity="few">少许</item>
        <item quantity="many">多数</item>
        <item quantity="other">其他</item>
    </plurals>
</resources>
```

The corresponding resources id to pack is mapping is like below:

```
{
    R.plurals.plural_list, 0,      
    R.string.hello_world, 1,      
}
```

Note: the file is created only for testing purpose, therefore the language tag doesn't follow the statndard rule with the standard Simplified or Traditional script.
