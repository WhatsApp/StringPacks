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
import androidx.annotation.Size;
import androidx.annotation.StringRes;
import java.io.IOException;
import java.io.InputStream;
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

  /** Registers map of app resource IDs to stringpack IDs. Called once at app start. */
  public void register(@NonNull @Size(multiple = 2) int[] idTable) {
    final int length = idTable.length;
    for (int i = 0; i < length; i += 2) {
      final int resId = idTable[i];
      final int packId = idTable[i + 1];
      resIdToPackIdMap.put(resId, packId);
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
          translation =
              parsedStringPack.getQuantityString(location, (long) quantity, pluralRules, false);
        } else {
          translation = parsedStringPack.getString(location, false);
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
  private static ParsedStringPack loadData(
      @NonNull Context context, @NonNull String fileName, @NonNull Locale locale) {
    ParsedStringPack result = null;
    try (InputStream inputStream = context.getAssets().open(fileName)) {
      final List<String> parentLocales = getParentLocales(locale);
      result = new ParsedStringPack(inputStream, parentLocales, null);
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
    parents.add(locale.getLanguage());
    return parents;
  }

  /** Provides a mechanism to decide which languages should use the default resources. */
  private static boolean useSystemResources(@NonNull Locale locale) {
    // US English uses the Android resources system, so don't load any pack for that.
    return locale.equals(Locale.US);
  }

  private static String getPackFileName(Locale locale) {
    return "strings_" + locale.getLanguage() + ".pack";
  }
}
