/* Copyright (c) Facebook, Inc. and its affiliates. All rights reserved.
 *
 * This source code is licensed under the Apache 2.0 license found in
 * the LICENSE file in the root directory of this source tree.
 */

package com.whatsapp.stringpacks.sample;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.whatsapp.stringpacks.StringPackResources;
import com.whatsapp.stringpacks.StringPackUtils;
import com.whatsapp.stringpacks.StringPacks;
import com.whatsapp.stringpacks.StringPacksLocaleMetaDataProvider;

import java.util.Locale;

public class SampleApplication extends Application {

  @Nullable private StringPackResources stringPackResources = null;
  @Nullable private final StringPacksLocaleMetaDataProvider metaDataProvider = new LocaleMetaDataProviderImpl();

  @Override
  protected void attachBaseContext(Context base) {
    StringPackIds.registerStringPackIds();
    StringPacks.registerStringPackLocaleMetaDataProvider(metaDataProvider);
    StringPacks.getInstance().setUp(base);
    super.attachBaseContext(base);
  }

  @Override
  public Resources getResources() {
    if (stringPackResources == null) {
      stringPackResources = StringPackResources.wrap(super.getResources());
    }
    return stringPackResources;
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
