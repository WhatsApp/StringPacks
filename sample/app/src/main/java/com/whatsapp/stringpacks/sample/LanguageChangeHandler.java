/* Copyright (c) Facebook, Inc. and its affiliates. All rights reserved.
 *
 * This source code is licensed under the Apache 2.0 license found in
 * the LICENSE file in the root directory of this source tree.
 */

package com.whatsapp.stringpacks.sample;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Provides methods to register/unregister for Device locale change. Listeners will have to implement
 * {@code OnDeviceLocaleChangeListener} and they will get a callback in {@code onDeviceLocaleChanged}
 * when the device language is changed
 */
public class LanguageChangeHandler {

  public interface OnDeviceLocaleChangeListener {
    void onDeviceLocaleChanged(Locale newLocale);
  }

  public static volatile LanguageChangeHandler INSTANCE = null;

  private final Set<OnDeviceLocaleChangeListener> deviceLocaleChangeListeners = new HashSet<>();

  public static LanguageChangeHandler getInstance() {
    if (INSTANCE == null) {
      synchronized (LanguageChangeHandler.class) {
        if (INSTANCE == null) {
          INSTANCE = new LanguageChangeHandler();
        }
      }
    }
    return INSTANCE;
  }

  public void registerOnDeviceLocaleChangeListeners(OnDeviceLocaleChangeListener listener) {
    deviceLocaleChangeListeners.add(listener);
  }

  public void unregisterOnDeviceLocaleChangeListener(OnDeviceLocaleChangeListener listener) {
    if (deviceLocaleChangeListeners.size() > 0) {
      deviceLocaleChangeListeners.remove(listener);
    }
  }

  public void notifyOnDeviceLocaleChangeListeners(Locale newLocale) {
    for (OnDeviceLocaleChangeListener listener : deviceLocaleChangeListeners) {
      listener.onDeviceLocaleChanged(newLocale);
    }
  }
}
