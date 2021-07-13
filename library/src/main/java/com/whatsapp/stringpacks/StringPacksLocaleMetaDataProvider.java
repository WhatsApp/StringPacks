package com.whatsapp.stringpacks;

import java.util.Locale;

public interface StringPacksLocaleMetaDataProvider {
    /**
     * Applications can return the appropriate pack file id for the locale,
     * based on their mapping in `pack_id_mapping` setting
     * @param locale
     * @return
     */
    String getPackFileIdForLocale(Locale locale);

    /**
     * This is a special case because we don't want to add language as a fallback locale
     * for some locales, for example, zh can not be a fallback locale for zh-TW
     * even if they are packed together, because zh is written in a different script than zh-TW.
     * Applications have to handle their special cases
     * and return true or false based on their specific cases.
     * @param locale
     * @return
     */
    boolean shouldAddLanguageAsParentForLocale(Locale locale);

    /**
     * Applications can decide whether to fallback to any specific locale
     * and not necessarily just the language
     * @param locale
     * @return
     */
    String getParentLocaleForLocale(Locale locale);
}
