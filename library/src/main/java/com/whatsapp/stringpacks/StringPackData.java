/* Copyright (c) Facebook, Inc. and its affiliates. All rights reserved.
 *
 * This source code is licensed under the Apache 2.0 license found in
 * the LICENSE file in the root directory of this source tree.
 */

package com.whatsapp.stringpacks;

import androidx.collection.ArraySet;
import java.nio.charset.Charset;
import java.util.Arrays;

public class StringPackData {
  private static final String REGION_ANGOLA = "AO";
  private static final String REGION_CAPE_VERDE = "CV";
  private static final String REGION_EQUITORIAL_GUINEA = "GQ";
  private static final String REGION_FRANCE = "FR";
  private static final String REGION_GUINEA_BISSAU = "GW";
  private static final String REGION_LUXEMBOURG = "LU";
  private static final String REGION_MACAU = "MO";
  private static final String REGION_MOZAMBIQUE = "MZ";
  private static final String REGION_PORTUGAL = "PT";
  private static final String REGION_SAO_TOME_AND_PRINCIPE = "ST";
  private static final String REGION_SWITZERLAND = "CH";
  private static final String REGION_TIMOR_LESTE = "TL";

  // List of Portuguese sublocales that map to pt-PT, from
  // CLDR's common/supplemental/supplementalData.xml, under <parentLocales>.
  public static final ArraySet<String> EUROPEAN_PORTUGUESE_LOCALES =
      new ArraySet<>(
          Arrays.asList(
              REGION_ANGOLA,
              REGION_CAPE_VERDE,
              REGION_EQUITORIAL_GUINEA,
              REGION_FRANCE,
              REGION_GUINEA_BISSAU,
              REGION_LUXEMBOURG,
              REGION_MACAU,
              REGION_MOZAMBIQUE,
              REGION_PORTUGAL,
              REGION_SAO_TOME_AND_PRINCIPE,
              REGION_SWITZERLAND,
              REGION_TIMOR_LESTE));

  static final int LOCALE_CODE_SIZE = 7;
  static final int HEADER_SIZE = 11;

  @SuppressWarnings("CharsetObjectCanBeUsed")
  static final Charset ASCII = Charset.forName("US-ASCII");

  // Keep in sync with `_ENCODING_ID` in string_pack.py
  @SuppressWarnings("CharsetObjectCanBeUsed")
  static final Charset[] ENCODINGS = {
      Charset.forName("UTF-8"), Charset.forName("UTF-16BE")
  };
}
