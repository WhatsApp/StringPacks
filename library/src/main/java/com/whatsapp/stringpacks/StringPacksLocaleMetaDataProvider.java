/* Copyright (c) Facebook, Inc. and its affiliates. All rights reserved.
 *
 * This source code is licensed under the Apache 2.0 license found in
 * the LICENSE file in the root directory of this source tree.
 */

package com.whatsapp.stringpacks;

import java.util.Locale;

/**
 * This interface specifies the methods that the application can override
 * to support region specific locales.
 *
 * If all the region specific locales are mapped in `pack_id_mapping` in
 * your config, then you don't need to implement this interface.
 */
public interface StringPacksLocaleMetaDataProvider {
    /**
     * Applications can return the appropriate pack file id for the locale,
     * based on their mapping in `pack_id_mapping` setting.
     *
     * This function identifies which pack file should the library read from for the specific locale.
     * for example,
     * // config.json
     * pack_id_mapping = {
     *   "es-rMX": "es",
     *   "es-rES": "es"
     * }
     *
     * If you can't map region specific locales into pack_id_mapping,
     * you should return the appropriate packFileId here.
     * For ex, zh-rTW and zh-rCN can't be mapped to zh due to different scripts
     * So, for locales "zh-TW" and "zh-CN", you should return
     * "zh-rTW" and "zh-rCN" respectively for the packFileId.
     *
     * @param locale - The new locale application switches to
     * @return packId or null if same as locale's language
     */
    String getPackFileIdForLocale(Locale locale);

    /**
     * This is a special case because we don't want to add language as a fallback locale
     * for some locales, for example, zh cannot be a fallback locale for zh-TW
     * even if they are packed together, because zh is written in a different script than zh-TW.
     * Applications have to handle their special cases
     * and return true or false based on their specific cases.
     * @param locale - The new locale application switches to
     * @return false for all special cases, true otherwise
     */
    boolean shouldAddLanguageAsParentForLocale(Locale locale);

    /**
     * Applications can decide whether to fallback to any specific locale
     * and not necessarily just the language.
     * For example, zh cannot be a fallback locale for zh-TW
     * @param locale - The new locale application switches to
     * @return The languageTag representing the locale for special cases, null otherwise
     */
    String getParentLocaleForLocale(Locale locale);
}
