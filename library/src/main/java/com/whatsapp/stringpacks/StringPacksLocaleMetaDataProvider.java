/* Copyright (c) Facebook, Inc. and its affiliates. All rights reserved.
 *
 * This source code is licensed under the Apache 2.0 license found in
 * the LICENSE file in the root directory of this source tree.
 */

package com.whatsapp.stringpacks;

import java.util.Locale;

/**
 * Implement this interface if your application is doing either of these:
 *
 * <p>1. Region specific translations, for example, having values-es-rES/strings.xml and
 * values-es-rMX/strings.xml, or
 *
 * <p>2. Packing translations for multiple locales into a single `.pack` file via `pack_id_mapping`,
 * or
 *
 * <p>3. Want fallback feature to work, for example, return `es` translation if a translation is
 * missing in `es-MX`
 */
public interface StringPacksLocaleMetaDataProvider {
  /**
   * Applications can return the appropriate pack file id for the locale, based on their mapping in
   * `pack_id_mapping` setting.
   *
   * <p>This function identifies which pack file should the library read from for the specific
   * locale. for example,
   *
   * <p>// config.json pack_id_mapping = { "es-rMX": "es", "es-rES": "es" }
   *
   * <p>If you can't map region specific locales into pack_id_mapping, you should return the
   * appropriate packFileId here.
   *
   * <p>For example, If you have a region specific `.pack` file, for example, `strings_es-rMX.pack`
   * In this case for `es-MX` locale you should return `es-rMX` as the pack file id.
   *
   * @param locale - Locale application wants to fetch the packId for
   * @return packId or null if same as locale's language
   */
  String getPackFileIdForLocale(Locale locale);

  /**
   * Whether the language could be a fallback locale for the specified locale.
   *
   * <p>Implementations can decide for their use-cases whether they want a locale to fallback to
   * language or not.
   *
   * <p>Note: Translations for both language (fallback locale) and the original locale MUST be
   * packed in the same pack file. Please take a look at `pack_id_mapping` property in config.json
   * For example, If the application has translations for both `es` and `es-MX` and want to show
   * `es` strings for the ones that are missing in `es-MX`, both `es` and `es-MX` should be packed
   * together
   *
   * <p>// config.json pack_id_mapping = { "es-rMX": "es" } and this method should return true for
   * `es-MX` locale
   *
   * <p>boolean shouldAddLanguageAsParentForLocale(Locale locale) { if
   * (locale.getLanguage().equals("es") && locale.getCountry().equalsIgnoreCase("MX")) { return
   * true; } return false; }
   *
   * <p>There may be some special cases where you may not want the language to be a fallback locale,
   * for example, `zh` should not be a fallback for `zh-TW` because their scripts are different.
   *
   * @param locale - Locale for which whether we want language as a fallback locale or not.
   * @return true if keeping language as fallback, false otherwise
   */
  boolean shouldAddLanguageAsParentForLocale(Locale locale);

  /**
   * Applications should override this method if they support translations for locales with region.
   * For a locale passed in this method, applications should return the locale whose translations
   * should be loaded from the pack file.
   *
   * <p>In combination with {@code shouldAddLanguageAsParentForLocale(Locale locale)}
   * implementations can define the most significant locale to pick translations of and a broader
   * fallback language, if a translation is not available.
   *
   * <p>For example, An app supports translations for `zh` and `zh-TW` Chinese locales only. So, if
   * the passed in locale to this method is `zh-HK`, you can decide to return `zh-TW`. In that case,
   * `zh-TW` translations will be shown when app's locale is `zh-HK`
   *
   * @param locale
   * @return The languageTag representing the locale in pack file, null otherwise
   */
  String getFirstChoiceLocaleInPackFileForLocale(Locale locale);
}
