/* Copyright (c) Facebook, Inc. and its affiliates. All rights reserved.
 *
 * This source code is licensed under the Apache 2.0 license found in
 * the LICENSE file in the root directory of this source tree.
 */

package com.whatsapp.stringpacks.sample;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.app.Application;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import androidx.test.core.app.ApplicationProvider;
import java.io.IOException;
import java.io.InputStream;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class LocaleUtilTest {

  @Mock Configuration configuration;
  @Mock Resources resources;
  @Mock AssetManager assetManager;

  private Application application;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    application = spy(ApplicationProvider.getApplicationContext());
    when(application.getResources()).thenReturn(resources);
    when(resources.getConfiguration()).thenReturn(configuration);
    when(resources.getAssets()).thenReturn(assetManager);
    try {
      InputStream inputStream = ApplicationProvider.getApplicationContext().getAssets().open("strings_zh.pack");
      when(assetManager.open("strings_zh.pack")).thenReturn(inputStream);
    } catch (IOException e) {
      Assert.fail("Test setup failure" + e);
    }
    try {
      InputStream inputStream = ApplicationProvider.getApplicationContext().getAssets().open("strings_ha-rNG.pack");
      when(assetManager.open("strings_ha-rNG.pack")).thenReturn(inputStream);
    } catch (IOException e) {
      Assert.fail("Test setup failure" + e);
    }
  }

  @Test
  public void testChineseLanguage() {
    LocaleUtil.overrideCustomLanguage(application, "zh");
    assertEquals(configuration.locale.getLanguage(), "zh");
  }

  @Test
  public void testUSEnglishLanguage() {
    LocaleUtil.overrideCustomLanguage(application, "en-US");
    assertEquals(configuration.locale.getLanguage(), "en");
    assertEquals(configuration.locale.getCountry(), "US");
  }

  @Test
  public void testHausaNigeriaLanguage() {
    LocaleUtil.overrideCustomLanguage(application, "ha-NG");
    assertEquals(configuration.locale.getLanguage(), "ha");
    assertEquals(configuration.locale.getCountry(), "NG");
  }
}
