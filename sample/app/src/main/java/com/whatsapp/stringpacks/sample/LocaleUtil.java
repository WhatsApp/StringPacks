/* Copyright (c) Facebook, Inc. and its affiliates. All rights reserved.
 *
 * This source code is licensed under the Apache 2.0 license found in
 * the LICENSE file in the root directory of this source tree.
 */

package com.whatsapp.stringpacks.sample;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import com.whatsapp.stringpacks.StringPacks;
import java.util.Locale;

public class LocaleUtil {

  private static final boolean UPDATE_CONFIGURATION_WORKS = Build.VERSION.SDK_INT < 26;

  public static void overrideCustomLanguage(Context context, String languageTag) {
    final Locale newLocale = new Locale(languageTag);
    final Context overriddenContext;

    if (UPDATE_CONFIGURATION_WORKS) {
      // On API < 26, we modify the existing Context directly.
      final Resources res = context.getResources();
      final Configuration config = res.getConfiguration();

      config.locale =  new Locale(languageTag);
      res.updateConfiguration(config, res.getDisplayMetrics());

      overriddenContext = context;
    } else {
      final Configuration config = new Configuration();
      config.setLocale(newLocale);

      overriddenContext = context.createConfigurationContext(config);
    }

    StringPacks.getInstance().setUp(overriddenContext);
  }
}
