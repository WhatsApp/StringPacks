/* Copyright (c) Facebook, Inc. and its affiliates. All rights reserved.
 *
 * This source code is licensed under the Apache 2.0 license found in
 * the LICENSE file in the root directory of this source tree.
 */

package com.whatsapp.stringpacks;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.app.Application;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class StringPacksTest {

  @NonNull private final Locale zhLocale = new Locale("zh");
  @NonNull private final Locale zhTWLocale = new Locale("zh", "TW");
  @NonNull private final Locale haNGLocale = new Locale("ha", "NG");
  @NonNull private final Locale enLocale = new Locale("en", "US");

  @Mock Resources resources;
  @Mock Configuration configuration;
  @Mock AssetManager assetManager;
  @Mock StringPacksLocaleMetaDataProvider stringPacksLocaleMetaDataProvider;

  private Application application;

  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    application = spy(ApplicationProvider.getApplicationContext());
    when(application.getResources()).thenReturn(resources);
    when(application.getApplicationContext()).thenReturn(null);
    when(resources.getConfiguration()).thenReturn(configuration);
    when(resources.getAssets()).thenReturn(assetManager);
    configuration.locale = zhLocale;
    try {
      InputStream inputStream = getClass().getClassLoader().getResourceAsStream("strings_zh.pack");
      when(assetManager.open("strings_zh.pack")).thenReturn(inputStream);
    } catch (IOException e) {
      Assert.fail("Test setup failure" + e);
    }
    try {
      InputStream inputStream = getClass().getClassLoader().getResourceAsStream("strings_zh-rTW.pack");
      when(assetManager.open("strings_zh-rTW.pack")).thenReturn(inputStream);
    } catch (IOException e) {
      Assert.fail("Test setup failure" + e);
    }
    try {
      InputStream inputStream = getClass().getClassLoader().getResourceAsStream("strings_ha.pack");
      when(assetManager.open("strings_ha.pack")).thenReturn(inputStream);
    } catch (IOException e) {
      Assert.fail("Test setup failure" + e);
    }
    StringPacks.getInstance()
        .register(new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15});
  }

  @Test
  public void testLanguageChangeFromChineseToEnglish() {
    StringPacks stringPacks = StringPacks.getInstance();
    // Setting up with Chinese
    stringPacks.setUp(application);
    String zhString = stringPacks.getString(StringPacksTestData.STRING_ID);
    assertEquals("你好，世界", zhString);

    // Switch language to en-US
    configuration.locale = enLocale;
    when(resources.getString(anyInt())).thenReturn("Test");
    stringPacks.setUp(application);
    String enString = stringPacks.getString(StringPacksTestData.STRING_ID);
    assertEquals("Test", enString);
  }

  @Test
  public void testLanguageChangeToChineseTaiwan() {
    StringPacks.registerStringPackLocaleMetaData(stringPacksLocaleMetaDataProvider);
    when(stringPacksLocaleMetaDataProvider.getPackFileIdForLocale(any())).thenReturn("zh");
    when(stringPacksLocaleMetaDataProvider.shouldAddLanguageAsParentForLocale(any())).thenReturn(true);
    StringPacks stringPacks = StringPacks.getInstance();
    // Setting up with Chinese
    stringPacks.setUp(application);
    String zhString = stringPacks.getString(StringPacksTestData.STRING_ID);
    assertEquals("你好，世界", zhString);

    // Switch language to zh-TW
    configuration.locale = zhTWLocale;
    when(stringPacksLocaleMetaDataProvider.getPackFileIdForLocale(any())).thenReturn("zh-rTW");
    when(stringPacksLocaleMetaDataProvider.shouldAddLanguageAsParentForLocale(any())).thenReturn(false);
    when(stringPacksLocaleMetaDataProvider.getParentLocaleForLocale(any())).thenReturn("zh-TW");
    when(resources.getString(anyInt())).thenReturn("世界你好！");
    stringPacks.setUp(application);
    String zhTaiwanString = stringPacks.getString(StringPacksTestData.STRING_ID);
    assertEquals("世界你好！", zhTaiwanString);
  }

  @Test
  public void testLanguageChangeToHausaNigeria() {
    StringPacks.registerStringPackLocaleMetaData(stringPacksLocaleMetaDataProvider);
    when(stringPacksLocaleMetaDataProvider.getPackFileIdForLocale(any())).thenReturn(null);
    when(stringPacksLocaleMetaDataProvider.shouldAddLanguageAsParentForLocale(any())).thenReturn(true);
    StringPacks stringPacks = StringPacks.getInstance();
    // Setting up with Chinese
    stringPacks.setUp(application);
    String zhString = stringPacks.getString(StringPacksTestData.STRING_ID);
    assertEquals("你好，世界", zhString);

    // Switch language to ha-NG
    configuration.locale = haNGLocale;
    when(stringPacksLocaleMetaDataProvider.getPackFileIdForLocale(any())).thenReturn(null);
    when(stringPacksLocaleMetaDataProvider.shouldAddLanguageAsParentForLocale(any())).thenReturn(false);
    when(stringPacksLocaleMetaDataProvider.getParentLocaleForLocale(any())).thenReturn("ha");
    when(resources.getString(anyInt())).thenReturn("Sannu Duniya!");
    stringPacks.setUp(application);
    String haNigeriaString = stringPacks.getString(StringPacksTestData.STRING_ID);
    assertEquals("Sannu Duniya!", haNigeriaString);
  }
}
