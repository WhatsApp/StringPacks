/* Copyright (c) Facebook, Inc. and its affiliates. All rights reserved.
 *
 * This source code is licensed under the Apache 2.0 license found in
 * the LICENSE file in the root directory of this source tree.
 */

package com.whatsapp.stringpacks.sample;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import androidx.annotation.NonNull;
import com.whatsapp.stringpacks.StringPackContext;
import com.whatsapp.stringpacks.StringPackUtils;
import com.whatsapp.stringpacks.StringPacks;
import com.whatsapp.stringpacks.utils.ContextUtils;
import java.util.Locale;

public class SampleApplication extends Application {

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

  @Override
  public void onConfigurationChanged(@NonNull Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    Locale current = StringPackUtils.getLocaleForContext(this);
    Locale newLocale = StringPackUtils.getLocaleFromConfiguration(newConfig);
    if (!current.toString().equals(newLocale.toString())) {
      LocaleUtil.overrideCustomLanguage(this, newLocale.getLanguage());
      LanguageChangeHandler.getInstance().notifyOnDeviceLocaleChangeListeners(newLocale);
    }
  }
}
