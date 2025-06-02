/* Copyright (c) Facebook, Inc. and its affiliates. All rights reserved.
 *
 * This source code is licensed under the Apache 2.0 license found in
 * the LICENSE file in the root directory of this source tree.
 */

package com.whatsapp.stringpacks;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import androidx.annotation.NonNull;
import java.util.Locale;

public class StringPackUtils {

  @NonNull
  public static Locale getLocaleFromConfiguration(@NonNull Configuration configuration) {
    Locale locale;
    if (Build.VERSION.SDK_INT >= 24) {
      if (configuration.getLocales().isEmpty()) {
        locale = Locale.getDefault();
      } else {
        locale = configuration.getLocales().get(0);
      }
    } else {
      locale = configuration.locale;
    }
    if (locale == null) {
      locale = Locale.getDefault();
      if (locale == null) {
        // This probably can't happen, but it's here to guarantee we never return null.
        locale = Locale.US;
      }
    }
    return locale;
  }

  @NonNull
  public static Locale getLocaleForContext(@NonNull Context context) {
    return getLocaleFromConfiguration(context.getResources().getConfiguration());
  }
}
