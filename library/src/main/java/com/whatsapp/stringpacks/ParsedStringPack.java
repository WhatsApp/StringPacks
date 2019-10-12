/* Copyright (c) Facebook, Inc. and its affiliates. All rights reserved.
 *
 * This source code is licensed under the Apache 2.0 license found in
 * the LICENSE file in the root directory of this source tree.
 */

package com.whatsapp.stringpacks;

import android.util.SparseArray;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Locale;

public class ParsedStringPack {
  @Nullable private SparseArray<String> strings;
  @Nullable private SparseArray<String[]> plurals;

  private static final Charset ASCII = Charset.forName("US-ASCII");

  // Keep in sync with `_ENCODING_ID` in string_pack.py
  private static final Charset[] ENCODINGS = {
    Charset.forName("UTF-8"), Charset.forName("UTF-16BE")
  };

  private static final int LOCALE_CODE_SIZE = 7;
  private static final int KIBIBYTE = 1024;

  private static final int HEADER_SIZE = 11;

  public ParsedStringPack(@NonNull InputStream inputStream, @NonNull List<String> parentLocales) {
    final byte[] input = byteArrayFromInputStream(inputStream);
    if (input == null) {
      SpLog.e("ParsedStringPack: could not read the language pack");
      return;
    }
    if (input.length < HEADER_SIZE) {
      SpLog.e("ParsedStringPack: header incomplete");
      return;
    }
    final int numLocales = read16BitsFrom(input, 0);
    final int startOfLocaleData = read32BitsFrom(input, 2);
    final byte encodingId = input[6];
    if (encodingId >= ENCODINGS.length) {
      SpLog.e("ParsedStringPack: unrecognized encoding");
    }
    final Charset encoding = ENCODINGS[encodingId];
    final int startOfStringData = read32BitsFrom(input, 7);
    if (input.length < startOfLocaleData || input.length < startOfStringData) {
      SpLog.e(
          String.format(
              Locale.US,
              "ParsedStringPack: header incomplete, input.length=%d startOfLocaleData=%d startOfStringData=%d",
              input.length,
              startOfLocaleData,
              startOfStringData));
      return;
    }

    if (parentLocales.isEmpty()) {
      SpLog.e("ParsedStringPack: parentLocales is empty");
      return;
    }

    int caret = HEADER_SIZE;
    int numMatches = 0;
    final int[] translationLocations = new int[parentLocales.size()];
    for (int i = 0; i < numLocales; i++) {
      final String resourceLocale = readLocaleFrom(input, caret);
      final int listIndex = parentLocales.indexOf(resourceLocale);
      if (listIndex != -1) { // Matching locale found
        numMatches++;
        translationLocations[listIndex] = caret; // Save the caret position for the match
        if (numMatches >= parentLocales.size()) {
          // No need to continue. We've already seen the maximum number of matches.
          break;
        }
      }
      caret += LOCALE_CODE_SIZE + 4;
    }
    for (int translationLocation : translationLocations) {
      if (translationLocation == 0) {
        // No translation found for the location at the corresponding index.
        continue;
      }
      // Since parentLocales is ordered from less specific to more specific, it's OK to
      // read the translations multiple times, since the later ones override the earlier ones.
      readTranslations(input, translationLocation, startOfLocaleData, startOfStringData, encoding);
    }
  }

  @Nullable
  private static byte[] byteArrayFromInputStream(@NonNull InputStream inputStream) {
    final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    final byte[] buffer = new byte[16 * KIBIBYTE];
    int bytesRead;
    try {
      while ((bytesRead = inputStream.read(buffer)) != -1) {
        byteArrayOutputStream.write(buffer, 0, bytesRead);
      }
    } catch (IOException exception) {
      SpLog.e("ParsedStringPack/byteArrayFromInputStream error: " + exception);
      return null;
    }
    return byteArrayOutputStream.toByteArray();
  }

  @NonNull
  private static String readLocaleFrom(@NonNull byte[] input, @IntRange(from = 0) int offset) {
    final int length;
    if (input[offset + 2] == '\0') {
      length = 2;
    } else if (input[offset + 5] == '\0') {
      length = 5;
    } else {
      length = LOCALE_CODE_SIZE;
    }
    return new String(input, offset, length, ASCII);
  }

  private static int read32BitsFrom(@NonNull byte[] input, @IntRange(from = 0) int offset) {
    return (input[offset] & 0xFF)
        | ((input[offset + 1] & 0xFF) << 8)
        | ((input[offset + 2] & 0xFF) << 16)
        | ((input[offset + 3] & 0xFF) << 24);
  }

  private static int read16BitsFrom(@NonNull byte[] input, @IntRange(from = 0) int offset) {
    return (input[offset] & 0xFF) | ((input[offset + 1] & 0xFF) << 8);
  }

  private void readTranslations(
      @NonNull byte[] input,
      @IntRange(from = 0) int caret,
      @IntRange(from = 0) int startOfLocaleData,
      @IntRange(from = 0) int startOfStringData,
      @NonNull Charset encoding) {
    caret += LOCALE_CODE_SIZE;
    final int headerStart = read32BitsFrom(input, caret);
    caret = startOfLocaleData + headerStart;
    if (input.length < caret + 4) {
      SpLog.e(
          String.format(
              Locale.US,
              "ParsedStringPack/readTranslations: header for locale incomplete, input.length=%d",
              input.length));
      return;
    }
    final int numStrings = read16BitsFrom(input, caret);
    caret += 2;
    final int numPlurals = read16BitsFrom(input, caret);
    caret += 2;
    if (input.length < caret + 10 * numStrings) {
      SpLog.e(
          String.format(
              Locale.US,
              "ParsedStringPack/readTranslations: header for locale incomplete, input.length=%d, caret=%d, numStrings=%d",
              input.length,
              caret,
              numStrings));
      return;
    }
    if (strings == null) {
      strings = new SparseArray<>(numStrings);
    }
    for (int i = 0; i < numStrings; i++) {
      final int id = read16BitsFrom(input, caret);
      caret += 2;
      final int stringStart = read32BitsFrom(input, caret);
      caret += 4;
      final int stringLen = read16BitsFrom(input, caret);
      caret += 2;
      strings.append(id, new String(input, startOfStringData + stringStart, stringLen, encoding));
    }
    if (plurals == null) {
      plurals = new SparseArray<>(numPlurals);
    }
    for (int i = 0; i < numPlurals; i++) {
      final int id = read16BitsFrom(input, caret);
      caret += 2;
      final int quantityCount = input[caret++];
      final String[] plural = new String[6];
      for (int j = 0; j < quantityCount; j++) {
        final int quantityId = input[caret++];
        final int stringStart = read32BitsFrom(input, caret);
        caret += 4;
        final int stringLen = read16BitsFrom(input, caret);
        caret += 2;
        plural[quantityId] =
            new String(input, startOfStringData + stringStart, stringLen, encoding);
      }
      plurals.append(id, plural);
    }
  }

  public boolean isEmpty() {
    return strings == null || plurals == null;
  }

  @Nullable
  public String getString(int id) {
    return strings == null ? null : strings.get(id);
  }

  // This must be kept in sync with the `_IDS_FOR_QUANTITY` dictionary in string_pack.py
  private static int quantityIndex(int quantity) {
    switch (quantity) {
      case PluralRules.QUANTITY_ZERO:
        return 1;
      case PluralRules.QUANTITY_ONE:
        return 2;
      case PluralRules.QUANTITY_TWO:
        return 3;
      case PluralRules.QUANTITY_FEW:
        return 4;
      case PluralRules.QUANTITY_MANY:
        return 5;
      default:
        return 0; // PluralRules.QUANTITY_OTHER
    }
  }

  @Nullable
  public String getQuantityString(int id, Object quantity, @NonNull PluralRules pluralRules) {
    if (plurals == null) {
      return null;
    }
    final String[] plural = plurals.get(id);
    if (plural == null) {
      return null;
    }
    final int index = quantityIndex(pluralRules.quantityForNumber(quantity));
    String result = plural[index];
    if (result != null) {
      return result;
    }
    // Fallback to QUANTITY_OTHER.
    return plural[0];
  }
}
