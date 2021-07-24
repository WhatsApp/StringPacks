/* Copyright (c) Facebook, Inc. and its affiliates. All rights reserved.
 *
 * This source code is licensed under the Apache 2.0 license found in
 * the LICENSE file in the root directory of this source tree.
 */

package com.whatsapp.stringpacks.sample;

import com.whatsapp.stringpacks.StringPacksLocaleMetaDataProvider;

import java.util.HashSet;
import java.util.Locale;

public class LocaleMetaDataProviderImpl implements StringPacksLocaleMetaDataProvider {
    private static final HashSet<String> REGION_PACK_IDS = new HashSet<String>() {{
        add("zh-rTW");
    }};

    @Override
    public String getPackFileIdForLocale(Locale locale) {
        String localeTag = String.format("%s-r%s", locale.getLanguage(), locale.getCountry());
        if (REGION_PACK_IDS.contains(localeTag))
            return localeTag;
        return null;
    }

    @Override
    public boolean shouldAddLanguageAsParentForLocale(Locale locale) {
        return !locale.equals(new Locale("zh", "TW"));
    }

    @Override
    public String getFirstChoiceLocaleInPackFileForLocale(Locale locale) {
        if (locale.getCountry().equals(""))
            return null;
        return String.format("%s-%s", locale.getLanguage(), locale.getCountry());
    }
}
