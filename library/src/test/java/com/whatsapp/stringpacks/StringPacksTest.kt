/* Copyright (c) Facebook, Inc. and its affiliates. All rights reserved.
 *
 * This source code is licensed under the Apache 2.0 license found in
 * the LICENSE file in the root directory of this source tree.
 */

package com.whatsapp.stringpacks

import android.app.Application
import android.content.res.AssetManager
import android.content.res.Configuration
import android.content.res.Resources
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.io.IOException
import java.util.Locale
import org.junit.Assert.fail
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mockito.spy
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

/** Tests for [StringPacks] locale setup and string resolution. */
@RunWith(RobolectricTestRunner::class)
class StringPacksTest {

  private val zhLocale = Locale("zh")
  private val zhTWLocale = Locale("zh", "TW")
  private val haNGLocale = Locale("ha", "NG")
  private val enLocale = Locale("en", "US")

  @Mock lateinit var resources: Resources
  @Mock lateinit var configuration: Configuration
  @Mock lateinit var assetManager: AssetManager
  @Mock lateinit var stringPacksLocaleMetaDataProvider: StringPacksLocaleMetaDataProvider

  private lateinit var application: Application
  private lateinit var closeable: AutoCloseable

  @Before
  fun setUp() {
    closeable = MockitoAnnotations.openMocks(this)
    application = spy(ApplicationProvider.getApplicationContext<Application>())
    whenever(application.resources).thenReturn(resources)
    whenever(application.applicationContext).thenReturn(null)
    whenever(resources.configuration).thenReturn(configuration)
    whenever(resources.assets).thenReturn(assetManager)
    try {
      val inputStream = requireNotNull(javaClass.classLoader) { "ClassLoader must not be null" }.getResourceAsStream("strings_zh.pack")
      whenever(assetManager.open("strings_zh.pack")).thenReturn(inputStream)
    } catch (e: IOException) {
      fail("Test setup failure$e")
    }
    try {
      val inputStream = requireNotNull(javaClass.classLoader) { "ClassLoader must not be null" }.getResourceAsStream("strings_zh-rTW.pack")
      whenever(assetManager.open("strings_zh-rTW.pack")).thenReturn(inputStream)
    } catch (e: IOException) {
      fail("Test setup failure$e")
    }
    try {
      val inputStream = requireNotNull(javaClass.classLoader) { "ClassLoader must not be null" }.getResourceAsStream("strings_ha.pack")
      whenever(assetManager.open("strings_ha.pack")).thenReturn(inputStream)
    } catch (e: IOException) {
      fail("Test setup failure$e")
    }
    StringPacks.getInstance()
        .register(intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15))
    StringPacks.registerStringPackLocaleMetaDataProvider(stringPacksLocaleMetaDataProvider)
  }

  @After
  fun tearDown() {
    closeable.close()
  }

  @Test
  fun testLanguageChangeFromChineseToEnglish() {
    whenever(stringPacksLocaleMetaDataProvider.getPackFileIdForLocale(any())).thenReturn("zh")
    whenever(stringPacksLocaleMetaDataProvider.shouldAddLanguageAsParentForLocale(any()))
        .thenReturn(true)
    val stringPacks = StringPacks.getInstance()
    configuration.locale = zhLocale
    stringPacks.setUp(application)
    val zhString = stringPacks.getString(StringPacksTestData.STRING_ID)
    assertThat(zhString).isEqualTo("你好，世界")

    configuration.locale = enLocale
    whenever(resources.getString(anyInt())).thenReturn("Test")
    stringPacks.setUp(application)
    val enString = stringPacks.getString(StringPacksTestData.STRING_ID)
    assertThat(enString).isEqualTo("Test")
  }

  @Test
  fun testSetUpApplicationInChineseTaiwan() {
    whenever(stringPacksLocaleMetaDataProvider.getPackFileIdForLocale(any())).thenReturn("zh-rTW")
    whenever(stringPacksLocaleMetaDataProvider.shouldAddLanguageAsParentForLocale(any()))
        .thenReturn(false)
    whenever(stringPacksLocaleMetaDataProvider.getFirstChoiceLocaleInPackFileForLocale(any()))
        .thenReturn("zh-TW")
    val stringPacks = StringPacks.getInstance()
    configuration.locale = zhTWLocale
    stringPacks.setUp(application)
    val zhTWString = stringPacks.getString(StringPacksTestData.STRING_ID)
    assertThat(zhTWString).isEqualTo("你好，世界")
  }

  @Test
  fun testSetUpApplicationInHausaNigeria() {
    whenever(stringPacksLocaleMetaDataProvider.getPackFileIdForLocale(any())).thenReturn("ha")
    whenever(stringPacksLocaleMetaDataProvider.shouldAddLanguageAsParentForLocale(any()))
        .thenReturn(true)
    whenever(stringPacksLocaleMetaDataProvider.getFirstChoiceLocaleInPackFileForLocale(any()))
        .thenReturn("ha-NG")
    val stringPacks = StringPacks.getInstance()
    configuration.locale = haNGLocale
    stringPacks.setUp(application)
    val haString = stringPacks.getString(StringPacksTestData.STRING_ID)
    assertThat(haString).isEqualTo("Sannu Duniya")
  }

  @Test
  fun testTranslationInFallbackLocale() {
    whenever(stringPacksLocaleMetaDataProvider.getPackFileIdForLocale(any())).thenReturn("ha")
    whenever(stringPacksLocaleMetaDataProvider.shouldAddLanguageAsParentForLocale(any()))
        .thenReturn(true)
    whenever(stringPacksLocaleMetaDataProvider.getFirstChoiceLocaleInPackFileForLocale(any()))
        .thenReturn("ha-NG")
    val stringPacks = StringPacks.getInstance()
    configuration.locale = haNGLocale
    stringPacks.setUp(application)
    val haString = stringPacks.getString(StringPacksTestData.FALLBACK_STRING_ID)
    assertThat(haString).isEqualTo("Barka dai arewacin amurka")
  }
}
