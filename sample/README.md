# StringPack Sample App

This app is used to demonstrate the StringPacks setup and functionality.


## Open the Project
In Android Studio welcome page, click "Open an existing Android Studio project", then choose the `sample/` directory.

After the project is loaded, click "Run" button, the `stringpacks` library should get included automatically and the app should successfully install in emulator or device, and all text should be displayed correctly, no matter they are from Android `strings.xml` resources file or `.pack` binary file.


## Move Strings for Packing

Whenever you add new translated string resources in the Sample project, run following command in the `sample/` directory to move packable strings to `string-packs` directory:

```
python3 ../library/scripts/assemble_string_packs.py --config ./app/src/main/string-packs/config.json
```

You can commit those string resources and moved files in the source control repository.

When run the project, the `.pack` files would be generated under `app/src/main/assets/` directory during build time. Therefore, we usually could ignore the `.pack` files from the source control repository.
