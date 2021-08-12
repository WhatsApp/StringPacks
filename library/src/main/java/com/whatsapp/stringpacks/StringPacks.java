/* Copyright (c) Facebook, Inc. and its affiliates. All rights reserved.
 *
 * This source code is licensed under the Apache 2.0 license found in
 * the LICENSE file in the root directory of this source tree.
 */

package com.whatsapp.stringpacks;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.PluralsRes;
import androidx.annotation.StringRes;
import com.whatsapp.stringpacks.utils.FileUtils;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/** This is the main interface for StringPacks */
public class StringPacks {
  private static volatile StringPacks INSTANCE;

  public static StringPacks getInstance() {
    if (INSTANCE == null) {
      synchronized (StringPacks.class) {
        if (INSTANCE == null) {
          INSTANCE = new StringPacks();
        }
      }
    }
    return INSTANCE;
  }

  private static final int NOT_PLURAL = -1;
  public static final String PACK_FILE_EXTENSION = ".pack";
  public static final String TEMP_PACK_FILE_EXTENSION = ".pack.tmp";
  public static final String TEMP_PACK_FILE = "extracted_pack_file" + TEMP_PACK_FILE_EXTENSION;
  private static final String UNDERSCORE = "_";

  @SuppressLint("UseSparseArrays")
  private final HashMap<Integer, Integer> resIdToPackIdMap = new HashMap<>();

  @NonNull private final Object stringPackLock = new Object();

  @GuardedBy("stringPackLock")
  private PluralRules pluralRules;

  @GuardedBy("stringPackLock")
  private Resources appRes;

  @GuardedBy("stringPackLock")
  @Nullable
  private ParsedStringPack parsedStringPack;

  @Nullable
  private static StringPacksLocaleMetaDataProvider stringPacksLocaleMetaDataProvider;

  /** Set up the library with latest locale at the start of the app, or on a locale change */
  public void setUp(@NonNull Context context) {
    // Read locale from context instead of appRes in case there is an overridden custom locale.
    final Locale locale = getLocaleFromContext(context.getResources());

    // TODO: Don't re-setup if the new locale is the same as the previous one.

    final boolean useStringPack = !useSystemResources(locale);

    appRes = resolveResources(context);

    synchronized (stringPackLock) {
      if (useStringPack) {
        parsedStringPack = loadData(context, getPackFileName(locale), locale);
      } else {
        parsedStringPack = null;
      }
      pluralRules = PluralRules.ruleForLocale(locale);
    }
  }

  private Resources resolveResources(Context context) {
    // It's first time set up from application, the base context passed in that has no application
    // set to it yet.
    if (context.getApplicationContext() != null) {
      context = context.getApplicationContext();
    }

    Resources resources = context.getResources();
    if (resources instanceof StringPackResources) {
      resources = ((StringPackResources) resources).getBaseResources();
    }

    if (resources == null) {
      throw new NullPointerException(
          "Can't find resources from context. This could happen if it's set up in "
              + "Application#attachBaseContext(), where the application doesn't have base context."
              + " If that's the case, you can just pass in the base context.");
    }

    return resources;
  }

  public static void registerStringPackLocaleMetaDataProvider(
      @Nullable StringPacksLocaleMetaDataProvider metaDataProvider) {
    stringPacksLocaleMetaDataProvider = metaDataProvider;
  }

  /** Registers map of app resource IDs to stringpack IDs. Called once at app start. */
  public void register(@NonNull int[] idTable) {
    final int length = idTable.length;
    for (int i = 0; i < length; i++) {
      final int resId = idTable[i];
      resIdToPackIdMap.put(resId, i);
    }
  }

  @Nullable
  public String getString(@StringRes int resId) {
    return getTranslation(resId, false, NOT_PLURAL);
  }

  @Nullable
  public String getQuantityString(@PluralsRes int resId, int quantity) {
    return getTranslation(resId, true, quantity);
  }

  @Nullable
  private String getTranslation(int resId, boolean isPlural, int quantity) {
    final Integer location = resIdToPackIdMap.get(resId);
    // This string was not moved to a StringPack.  Fall back to default strings.
    if (location == null) {
      return fallback(resId, isPlural, quantity);
    }

    String translation = null;
    synchronized (stringPackLock) {
      if (parsedStringPack != null) {
        if (isPlural) {
          // TODO better fix for the int / long interfaces.
          translation = parsedStringPack.getQuantityString(location, (long) quantity, pluralRules);
        } else {
          translation = parsedStringPack.getString(location);
        }
      }
    }

    // StringPack has not been initialized yet, fall back to default strings.
    return translation != null ? translation : fallback(resId, isPlural, quantity);
  }

  private String fallback(int resId, boolean isPlural, int quantity) {
    if (isPlural) {
      return appRes.getQuantityString(resId, quantity);
    } else {
      return appRes.getString(resId);
    }
  }

  @Nullable
  static ParsedStringPack loadData(
      @NonNull Context context, @NonNull String fileName, @NonNull Locale locale) {
    ParsedStringPack result = null;
    String resourcePackFileName = fileName + PACK_FILE_EXTENSION;

    try {
      MappedByteBuffer mappedByteBuffer = null;
      File extractedPackFile =
          extractPackFile(context, fileName, context.getResources(), resourcePackFileName);
      RandomAccessFile randomAccessFile = new RandomAccessFile(extractedPackFile, "r");
      FileChannel fileChannel = randomAccessFile.getChannel();
      mappedByteBuffer =
          fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, extractedPackFile.length());
      final List<String> parentLocales = getParentLocales(locale);
      result = new ParsedStringPack(parentLocales, mappedByteBuffer);
    } catch (IOException exception) {
      SpLog.e("translations/loadData error:" + exception);
    }
    return result;
  }

  @NonNull
  private static Locale getLocaleFromContext(@NonNull Resources resources) {
    return StringPackUtils.getLocaleFromConfiguration(resources.getConfiguration());
  }

  /**
   * Provides a mechanism for a locale hierarchy. This may need customization based on what the app
   * is localized to (for example, zh-TW should not have zh as its parent, because they're written
   * in different scripts).
   */
  private static List<String> getParentLocales(@NonNull Locale locale) {
    final ArrayList<String> parents = new ArrayList<>();
    if (stringPacksLocaleMetaDataProvider == null
        || stringPacksLocaleMetaDataProvider.shouldAddLanguageAsParentForLocale(locale)) {
      parents.add(locale.getLanguage());
    }
    if (stringPacksLocaleMetaDataProvider != null) {
      String parent = stringPacksLocaleMetaDataProvider.getFirstChoiceLocaleInPackFileForLocale(locale);
      if (parent != null)
        parents.add(parent);
    }
    return parents;
  }

  /** Provides a mechanism to decide which languages should use the default resources. */
  private static boolean useSystemResources(@NonNull Locale locale) {
    // US English uses the Android resources system, so don't load any pack for that.
    return locale.equals(Locale.US);
  }

  private static String getPackFileName(Locale locale) {
    String packFileId = null;
    if (stringPacksLocaleMetaDataProvider != null) {
      packFileId = stringPacksLocaleMetaDataProvider.getPackFileIdForLocale(locale);
    }
    if (packFileId == null) {
      packFileId = locale.getLanguage();
    }
    return "strings_" + packFileId;
  }

  /**
   * Extract a pack file to internal files directory for mmapping.
   *
   * @param context
   * @param fileName file name of the pack file
   * @param resources resources in which pack file is stored
   * @param resourcePackFileName resource pack file name
   * @return extracted pack file handle
   * @throws IOException
   */
  @NonNull
  private static File extractPackFile(
      final Context context,
      @NonNull String fileName,
      @NonNull Resources resources,
      @NonNull String resourcePackFileName)
      throws IOException {
    File filesDirectory = context.getFilesDir();
    String extractedPackFileName =
        fileName + UNDERSCORE + getPackageCodePathTimestamp(context) + PACK_FILE_EXTENSION;
    File extractedPackFile = new File(filesDirectory, extractedPackFileName);

    if (!extractedPackFile.exists()) {
      File tempFile = new File(filesDirectory, TEMP_PACK_FILE);
      OutputStream out = new FileOutputStream(tempFile);
      FileUtils.copyStream(resources.getAssets().open(resourcePackFileName), out);
      out.close();
      boolean rename = tempFile.renameTo(extractedPackFile);
      if (!rename) {
        throw new IOException("Renaming temp file failed");
      }
    }
    return extractedPackFile;
  }

  /**
   * Return the timestamp of the package code path. This is used to differentiate two version
   * installations of the app.
   *
   * @param context
   * @return
   */
  private static long getPackageCodePathTimestamp(final Context context) {
    return new File(context.getPackageCodePath()).lastModified() / 1000;
  }

  /**
   * Clean up old pack files from internal file storage
   *
   * @param context
   */
  public static void cleanupOldPackFiles(Context context) {
    File filesDirectory = context.getFilesDir();
    FilenameFilter filenameFilter =
        (dir, name) ->
            name.endsWith(PACK_FILE_EXTENSION) || name.endsWith(TEMP_PACK_FILE_EXTENSION);

    String[] filesNames = filesDirectory.list(filenameFilter);

    if (filesNames != null) {
      for (String fileName : filesNames) {
        String filePrefix = fileName.substring(0, fileName.lastIndexOf(PACK_FILE_EXTENSION));
        String[] splitName = filePrefix.split(UNDERSCORE);
        if (splitName.length > 1) {
          try {
            if (Long.parseLong(splitName[splitName.length - 1])
                != getPackageCodePathTimestamp(context)) {
              SpLog.i("translations/cleanupOldPackFiles Clearing old pack file: " + fileName);
              boolean isDeleted = new File(filesDirectory, fileName).delete();
              if (!isDeleted) {
                SpLog.e(
                    "translations/cleanupOldPackFiles Could not delete old pack file: " + fileName);
              }
            }
          } catch (NumberFormatException e) {
            SpLog.w(
                "translations/cleanupOldPackFiles Pack file name did not contain version info: "
                    + fileName);
          }
        }
      }
    }
  }
}
