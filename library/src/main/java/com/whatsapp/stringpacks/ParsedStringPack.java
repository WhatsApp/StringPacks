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
  @NonNull private SparseArray<String> strings = new SparseArray<>();
  @NonNull private SparseArray<String[]> plurals = new SparseArray<>();
  private int startOfStringData;
  private Charset encoding;
  @NonNull private final byte[] input;
  private int[] headerLocations;

  private static final Charset ASCII = Charset.forName("US-ASCII");

  // Keep in sync with `_ENCODING_ID` in string_pack.py
  private static final Charset[] ENCODINGS = {
    Charset.forName("UTF-8"), Charset.forName("UTF-16BE")
  };

  private static final int LOCALE_CODE_SIZE = 7;
  private static final int KIBIBYTE = 1024;

  private static final int HEADER_SIZE = 11;

  public ParsedStringPack(@NonNull InputStream inputStream, @NonNull List<String> parentLocales) {
    final byte[] byteArray = byteArrayFromInputStream(inputStream);
    if (byteArray == null) {
      SpLog.e("ParsedStringPack: could not read the language pack");
      input = new byte[0]; // Just to make sure we assign something to satisfy @NonNull
      return;
    } else {
      input = byteArray;
    }
    if (input.length < HEADER_SIZE) {
      SpLog.e("ParsedStringPack: header incomplete");
      return;
    }
    final int numLocales = read16BitsFrom(0);
    final int startOfLocaleData = read32BitsFrom(2);
    final byte encodingId = input[6];
    if (encodingId >= ENCODINGS.length) {
      SpLog.e("ParsedStringPack: unrecognized encoding");
    }
    encoding = ENCODINGS[encodingId];
    startOfStringData = read32BitsFrom(7);
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
      final String resourceLocale = readLocaleFrom(caret);
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

    int headerLocationIndex = 0;
    headerLocations = new int[numMatches];
    for (int translationLocation : translationLocations) {
      if (translationLocation == 0) {
        continue;
      }
      final int headerStart = read32BitsFrom(translationLocation + LOCALE_CODE_SIZE);
      headerLocations[headerLocationIndex] = startOfLocaleData + headerStart;
      if (input.length < headerLocations[headerLocationIndex] + 4) {
        SpLog.e(
            String.format(
                Locale.US,
                "ParsedStringPack: header for locale incomplete, input.length=%d",
                input.length));
        return;
      }
      headerLocationIndex++;
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
  private String readLocaleFrom(@IntRange(from = 0) int offset) {
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

  private int read32BitsFrom(@IntRange(from = 0) int offset) {
    return (input[offset] & 0xFF)
        | ((input[offset + 1] & 0xFF) << 8)
        | ((input[offset + 2] & 0xFF) << 16)
        | ((input[offset + 3] & 0xFF) << 24);
  }

  private int read16BitsFrom(@IntRange(from = 0) int offset) {
    return (input[offset] & 0xFF) | ((input[offset + 1] & 0xFF) << 8);
  }

  @Nullable
  private String findString(int headerLocation, int id) {
    int caret = headerLocation;
    final int numStrings = read16BitsFrom(caret);
    // Skip 2 bytes for the number of strings, and another 2 bytes for the number of plurals, which
    // we don't care about here.
    caret += 4;
    if (input.length < caret + 8 * numStrings) {
      SpLog.e(
          String.format(
              Locale.US,
              "ParsedStringPack/readTranslations: header for locale incomplete, input.length=%d, caret=%d, numStrings=%d",
              input.length,
              caret,
              numStrings));
      return null;
    }

    // Binary search for the value of ID.
    int left = 0;
    int right = numStrings - 1;
    while (left <= right) {
      final int middle = (left + right) / 2;
      final int middleCaret = caret + 8 * middle;
      final int middleId = read16BitsFrom(middleCaret);
      if (middleId < id) {
        left = middle + 1;
      } else if (middleId > id) {
        right = middle - 1;
      } else {
        final int stringStart = read32BitsFrom(middleCaret + 2);
        final int stringLen = read16BitsFrom(middleCaret + 6);
        return new String(input, startOfStringData + stringStart, stringLen, encoding);
      }
    }
    // If we are here, we didn't find the string ID.
    return null;
  }

  @Nullable
  private String[] findPlural(int headerLocation, int id) {
    int caret = headerLocation;
    final int numStrings = read16BitsFrom(caret);
    caret += 2;
    final int numPlurals = read16BitsFrom(caret);
    caret += 2;
    if (input.length < caret + 8 * numStrings) {
      SpLog.e(
          String.format(
              Locale.US,
              "ParsedStringPack/readTranslations: header for locale incomplete, input.length=%d, caret=%d, numStrings=%d",
              input.length,
              caret,
              numStrings));
      return null;
    }
    // Skip the information for all normal (non-plural) strings, which we don't need.
    caret += 8 * numStrings;
    // TODO(roozbehp):
    // The present plural format is not binary-search friendly for plurals. Change the binary
    // format and make this a binary search.
    for (int i = 0; i < numPlurals; i++) {
      final int pluralId = read16BitsFrom(caret);
      caret += 2;
      final int quantityCount = input[caret++];
      if (pluralId == id) {
        final String[] plural = new String[6];
        for (int j = 0; j < quantityCount; j++) {
          final int quantityId = input[caret++];
          final int stringStart = read32BitsFrom(caret);
          caret += 4;
          final int stringLen = read16BitsFrom(caret);
          caret += 2;
          plural[quantityId] =
              new String(input, startOfStringData + stringStart, stringLen, encoding);
        }
        return plural;
      } else {
        // For each quantity, skip 7 bytes: 1 byte for the quantity ID, 4+2 bytes for the string
        // location in the string pool.
        caret += 7 * quantityCount;
      }
    }
    // If we are here, we didn't find the plural ID.
    return null;
  }

  @Nullable
  private String loadString(int id) {
    // Start from the most specific locale, which is at the end of the array, and find the first
    // translation.
    for (int i = headerLocations.length - 1; i >= 0; i--) {
      final String translation = findString(headerLocations[i], id);
      if (translation != null) {
        strings.put(id, translation);
        return translation;
      }
    }
    return null;
  }

  @Nullable
  private String[] loadPlural(int id) {
    for (int i = headerLocations.length - 1; i >= 0; i--) {
      final String[] plural = findPlural(headerLocations[i], id);
      if (plural != null) {
        plurals.put(id, plural);
        return plural;
      }
    }
    return null;
  }

  public boolean isEmpty() {
    // TODO(roozbehp): Investigate if we need to be more conservative and actually check for data.
    return headerLocations.length == 0;
  }

  @Nullable
  public String getString(int id) {
    final String result = strings.get(id);
    if (result == null) {
      // String not loaded or doesn't exist.
      return loadString(id);
    } else {
      return result;
    }
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
    String[] plural = plurals.get(id);
    if (plural == null) {
      // Plural set not loaded or doesn't exist.
      plural = loadPlural(id);
    }
    if (plural == null) {
      // It doesn't exist.
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
