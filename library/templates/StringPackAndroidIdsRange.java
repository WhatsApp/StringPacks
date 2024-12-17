/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package {package.name};

import androidx.annotation.Nullable;
import androidx.annotation.PluralsRes;
import androidx.annotation.StringRes;

public class StringPackAndroidIdsRange {
  // region StringPacks ID range
  // endregion

  public @Nullable Integer getStringPackStringId(@StringRes int resId) {
    //noinspection ResourceType
    if (resId < STRING_BEGIN || resId > STRING_END) {
      return null;
    }
    return resId - STRING_BEGIN + pluralsLength;
  }

  public @Nullable Integer getStringPackPluralsId(@PluralsRes int resId) {
    //noinspection ResourceType
    if (resId < PLURALS_BEGIN || resId > PLURALS_END) {
      return null;
    }
    return resId - PLURALS_BEGIN;
  }
}
