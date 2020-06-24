# Unit Test Resources

This directory contains all the resources that needed for unit tests.

## strings_zh.pack

This file is generated from `res/values-zh/strings.xml` with following content.

```xml
<?xml version="1.0" encoding="utf-8"?>

<resources>
    <string name="hello_world">你好，世界</string>
    <string name="hello_africa">你好非洲</string>
    <string name="hello_asia">你好亚洲</string>
    <string name="hello_antartica">南极洲你好</string>
    <string name="hello_north_america">你好，北美</string>
    <string name="hello_south_america">您好，南美</string>
    <string name="hello_australia">澳大利亚你好</string>
    <string name="hello_europe">您好欧洲</string>
    <string name="hello_atlantic">你好大西洋</string>
    <string name="hello_pacific">你好太平洋</string>
    <string name="hello_arctic">你好北冰洋</string>
    <string name="hello_indian">你好印度洋</string>
    <string name="hello_rivers">你好河</string>
    <string name="hello_mountains">你好山</string>
    <string name="hello_forests">你好森林</string>
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
    R.string.hello_africa, 1,
    R.string.hello_antartica, 2,
    R.string.hello_arctic, 3,
    R.string.hello_asia, 4,
    R.string.hello_atlantic, 5,
    R.string.hello_australia, 6,
    R.string.hello_europe, 7,
    R.string.hello_forests, 8,
    R.string.hello_indian, 9,
    R.string.hello_mountains, 10,
    R.string.hello_north_america, 11,
    R.string.hello_pacific, 12,
    R.string.hello_rivers, 13,
    R.string.hello_south_america, 14,
    R.string.hello_world, 15,
}
```

Note: the file is created only for testing purpose, therefore the language tag doesn't follow the statndard rule with the standard Simplified or Traditional script.
