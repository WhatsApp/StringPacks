# StringPacks

StringPacks is a library to store translation strings in a more efficient binary format for Android applications, so that it reduces the Android APK size.

Check out [our tech talk on StringPacks from DroiCon SF 2019](https://youtu.be/npnamYPQD3g?t=812) to know more about the motivation, architecture and prospect of the StringPacks project.

## Requirements

- **Python 3** - The StringPacks python scripts are written in Python 3.
- **minSdkVersion 15** - The library default min sdk version is 15, but it should work for lower SDK versions.
- **Git** - The script uses `git ls-files` to look up files.
- **Android development environment**
- **Gradle Build System**


## Setup in Android Project

1. Copy the [scripts/](library/scripts/) and [pack.gradle](library/pack.gradle) from `library/` to the root directory of your Android project.
2. Move either [Java](library/templates/StringPackIds.java) or [Kotlin](library/templates/StringPackIds.kt) version of `StringPackIds` file from [templates/](library/templates/) directory to your project source code directory.
    - Edit package information of the file.
3. Move [template config.json](library/templates/config.json) to your Android application project directory.
    - Replace `{app}` to be your application project directory name.
    - Point `pack_ids_class_file_path` to the path where you put the `StringPackIds` file.
4. Make following changes to your Android project's `build.gradle`.
   ```
   allprojects {

     repositories {
       ...
       mavenCentral()
    }
    ...
   }

   // Replace `{path_to_config.json}` with the path to your `config.json` file
   ext {
     stringPacksConfigFile = "$rootDir/{path_to_config.json}"
   }
   ```
   - Replace `{path_to_config.json}` with the path to your `config.json` file
5. Make following changes to your Android application's `build.gradle`
   ```
   apply from: "$rootDir/pack.gradle"

   dependencies {
     ...
     ...
     implementation 'com.whatsapp.stringpacks:stringpacks:0.2.1'
   }
   ```

You now have StringPacks available in your Android project.

## Getting Started

There are a few steps to walk through before you can really use packed strings in your application. But don't worry, most of them only need to be done once.

### Runtime

Since the translated strings are moved to our special binary format (`.pack` files), your application needs a way to read those strings during runtime. The library provides a wrapper class for [`Context`](https://developer.android.com/reference/android/content/ContextWrapper) and [`Resources`](https://developer.android.com/reference/android/content/res/Resources) to help with that.

You need to add the following code to all subclasses of your Context class (like [`Activity`](https://developer.android.com/reference/android/app/Activity) and [`Service`](https://developer.android.com/reference/android/app/Service)) to ensure the strings are read from `.pack` files instead of Android system resources.

```java
// Java

@Override
protected void attachBaseContext(Context base) {
    super.attachBaseContext(StringPackContext.wrap(base));
}
```

```kotlin
// Kotlin

override fun attachBaseContext(base: Context?) {
    super.attachBaseContext(StringPackContext.wrap(base))
}
```

If all of the following conditions meet, you need to override `getResources()` function also in your `Activity`
1. App's `minSdkVersion` is < 17
2. You have a dependency on `androidx.appcompat:appcompat:1.2.0`
3. Your Activity extends from [`AppCompatActivity`](https://developer.android.com/reference/androidx/appcompat/app/AppCompatActivity)

```java
// Java

private @Nullable StringPackResources stringPackResources;
@Override
public Resources getResources() {
  if (stringPackResources == null) {
    stringPackResources = StringPackResources.wrap(super.getResources());
  }
  return stringPackResources;
}
```

```kotlin
// Kotlin

private @Nullable var stringPackResources:Resources? = null
override fun getResources(): Resources? {
  if (stringPackResources == null) {
    stringPackResources = StringPackResources.wrap(super.getResources())
  }
  return stringPackResources
}
```

Your Android application also needs to use a custom [`Application`](https://developer.android.com/reference/android/app/Application), which needs to include the following code to ensure the strings are read from `.pack` files.

```java
// Java

@Override
protected void attachBaseContext(Context base) {
  StringPackIds.registerStringPackIds();
  StringPacks.getInstance().setUp(base);

  super.attachBaseContext(StringPackContext.wrap(base));
}

@Override
public Context getBaseContext() {
  return ContextUtils.getRootContext(super.getBaseContext());
}
```

```kotlin
// Kotlin

override fun attachBaseContext(base: Context?) {
    registerStringPackIds()
    StringPacks.getInstance().setUp(base)

    super.attachBaseContext(StringPackContext.wrap(base))
}

override fun getBaseContext(): Context {
  return ContextUtils.getRootContext(super.getBaseContext())
}
```

You only need to do this each time you add a new context component. You don't need to do this for each component if you add them to a base class.

### Generate `.pack` files

You have added the `StringPackIds` file to your project, but it has nothing in it yet. It is supposed to hold the mapping from android resource IDs (`R.string`) to string pack IDs.
The content would be automatically filled in when you run the script that provided by this library.
The mapping information would also be used for generating the `.pack` files, so they are correctly loaded at runtime.

Execute the python script from your project root directory to assemble the string packs:
```bash
python3 ./scripts/assemble_string_packs.py --config ./{path_to}/config.json
```

You will see:

- The `StringPackIds` file has been updated with the pack ID mapping information;
- The translation strings, which are packable, have been moved to different directory, so that they won't be compiled into the APK;
- The `.pack` file for different language have been generated under the project assets/ directory.

When you update translations, or change a string in the project, you may run the script again to generate `.pack` files with latest content.

Those string resource IDs that are not listed in the `StringPackIds` file, will continue to be kept in the Android system resources, and the StringPacks runtime would automatically fall back to read from there.

&nbsp;

Now, you can use gradle to build your application as usual. The application should correctly retrieve the strings from StringPacks.

## License
```
Copyright (c) Facebook, Inc. and its affiliates.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
