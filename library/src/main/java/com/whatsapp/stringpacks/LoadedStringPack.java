package com.whatsapp.stringpacks;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Locale;

/** This class holds the logic that loads whole string pack content from input stream directly. */
class LoadedStringPack {
  private static final int KIBIBYTE = 1024; // 2^10

  private byte[] input;
  private Charset encoding;
  private int startOfStringData;
  private int[] headerLocations; // locations of matched locales' headers

  public LoadedStringPack(@NonNull InputStream inputStream, @NonNull List<String> parentLocales) {

    final byte[] byteArray = byteArrayFromInputStream(inputStream);
    if (byteArray == null) {
      SpLog.e("LoadedStringPack: could not read the language pack");
      return;
    } else {
      input = byteArray;
    }

    if (input.length < StringPackData.HEADER_SIZE) {
      SpLog.e("LoadedStringPack: header incomplete");
      return;
    }

    final int numLocales = read16BitsFrom(0);
    final int startOfLocaleData = read32BitsFrom(2);
    final byte encodingId = input[6];

    if (encodingId >= StringPackData.ENCODINGS.length) {
      SpLog.e("LoadedStringPack: unrecognized encoding");
    }
    encoding = StringPackData.ENCODINGS[encodingId];
    startOfStringData = read32BitsFrom(7);

    if (input.length < startOfLocaleData || input.length < startOfStringData) {
      SpLog.e(
          String.format(
              Locale.US,
              "LoadedStringPack: header incomplete, input.length=%d startOfLocaleData=%d startOfStringData=%d",
              input.length,
              startOfLocaleData,
              startOfStringData));
      return;
    }

    if (parentLocales.isEmpty()) {
      SpLog.e("LoadedStringPack: parentLocales is empty");
      return;
    }

    int caret = StringPackData.HEADER_SIZE;
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
      caret += StringPackData.LOCALE_CODE_SIZE + 4;
    }

    int headerLocationIndex = 0;
    headerLocations = new int[numMatches];
    for (int translationLocation : translationLocations) {
      if (translationLocation == 0) {
        continue;
      }
      final int headerStart;

      headerStart = read32BitsFrom(translationLocation + StringPackData.LOCALE_CODE_SIZE);

      headerLocations[headerLocationIndex] = startOfLocaleData + headerStart;

      if (input.length < headerLocations[headerLocationIndex] + 4) {
        SpLog.e(
            String.format(
                Locale.US,
                "LoadedStringPack: header for locale incomplete, input.length=%d",
                input.length));
        return;
      }

      headerLocationIndex++;
    }
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

  @NonNull
  private String readLocaleFrom(@IntRange(from = 0) int offset) {
    final int length;
    if (input[offset + 2] == '\0') {
      length = 2;
    } else if (input[offset + 5] == '\0') {
      length = 5;
    } else {
      length = StringPackData.LOCALE_CODE_SIZE;
    }
    return new String(input, offset, length, StringPackData.ASCII);
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
    // TODO(roozbehp):
    // Replace this linear search with a binary search after making sure the IDs are sorted when
    // packing the strings.
    for (int i = 0; i < numStrings; i++) {
      if (read16BitsFrom(caret) == id) {
        caret += 2;
        final int stringStart = read32BitsFrom(caret);
        caret += 4;
        final int stringLen = read16BitsFrom(caret);
        return new String(input, startOfStringData + stringStart, stringLen, encoding);
      } else {
        // 2 bytes for the string id, 4 bytes for the stringStart, and 2 bytes for the string
        // length. So we skip 8 bytes altogether.
        caret += 8;
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
              "LoadedStringPack/readTranslations: header for locale incomplete, input.length=%d, caret=%d, numStrings=%d",
              input.length,
              caret,
              numStrings));
      return null;
    }
    // Skip the information for all normal (non-plural) strings, which we don't need.
    caret += 8 * numStrings;
    // TODO(roozbehp):
    // Replace this linear search with a binary search after making sure the IDs are sorted when
    // packing the strings.
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

  public String loadString(int id) {
    // Start from the most specific locale, which is at the end of the array, and find the first
    // translation.
    for (int i = headerLocations.length - 1; i >= 0; i--) {
      final String translation = findString(headerLocations[i], id);
      if (translation != null) {
        return translation;
      }
    }
    return null;
  }

  @Nullable
  public String[] loadPlural(int id) {
    for (int i = headerLocations.length - 1; i >= 0; i--) {
      final String[] plural = findPlural(headerLocations[i], id);
      if (plural != null) {
        return plural;
      }
    }
    return null;
  }

  public boolean isEmpty() {
    // TODO(roozbehp): Investigate if we need to be more conservative and actually check for data.
    return headerLocations == null || headerLocations.length == 0;
  }

  @VisibleForTesting
  @Nullable
  static byte[] byteArrayFromInputStream(@NonNull InputStream inputStream) {
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
}
