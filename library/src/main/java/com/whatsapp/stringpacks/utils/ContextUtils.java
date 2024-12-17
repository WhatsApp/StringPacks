/* Copyright (c) Facebook, Inc. and its affiliates. All rights reserved.
 *
 * This source code is licensed under the Apache 2.0 license found in
 * the LICENSE file in the root directory of this source tree.
 */

package com.whatsapp.stringpacks.utils;

import android.content.Context;
import android.content.ContextWrapper;
import androidx.annotation.Nullable;

public class ContextUtils {

  /**
   * Fetch the base {@code Context} as close as possible to the system to fetch Android system
   * resources
   *
   * @return base Context
   */
  @Nullable
  public static Context getRootContext(@Nullable Context context) {
    Context base = context;
    while (base != null && base instanceof ContextWrapper) {
      Context newBase = ((ContextWrapper) base).getBaseContext();
      if (newBase == null) {
        break;
      }
      base = newBase;
    }
    return base;
  }
}
