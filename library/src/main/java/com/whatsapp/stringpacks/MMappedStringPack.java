package com.whatsapp.stringpacks;

import android.util.SparseIntArray;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.nio.MappedByteBuffer;
import java.nio.charset.Charset;
import java.util.List;

/**
 * Class that holds logic for MMapped string pack
 */
public class MMappedStringPack {

  private final MappedByteBuffer mappedByteBuffer;
  private final Charset encoding;
  private final int startOfStringData;

  //The following arrays store the id->location data for strings and plurals
  private final SparseIntArray pluralSparseArray = new SparseIntArray();
  private final SparseIntArray stringSparseArray = new SparseIntArray();

  public MMappedStringPack (
      @NonNull List<String> parentLocales,
      @NonNull MappedByteBuffer mappedPackFile) {
    mappedByteBuffer = mappedPackFile;

    final int numLocales = read16BitsFrom(0);
    final int startOfLocaleData = read32BitsFrom(2);
    final byte encodingId = mappedByteBuffer.get(6);

    if (encodingId >= StringPackData.ENCODINGS.length) {
      SpLog.e("MMappedStringPack: unrecognized encoding");
    }

    encoding = StringPackData.ENCODINGS[encodingId];
    startOfStringData = read32BitsFrom(7);

    if (parentLocales.isEmpty()) {
      SpLog.e("MMappedStringPack: parentLocales is empty");
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

    for (int translationLocation : translationLocations) {
      if (translationLocation == 0) {
        continue;
      }
      final int headerStart;

      mappedByteBuffer.position(translationLocation + StringPackData.LOCALE_CODE_SIZE);
      headerStart = read32BitsFrom(mappedByteBuffer.position());

      //We will map the translation location here from less specific to more specific
      mapTranslations(startOfLocaleData, headerStart);
    }
  }

  private int read16BitsFrom(@IntRange(from = 0) int offset) {
    return (mappedByteBuffer.get(offset) & 0xFF) | ((mappedByteBuffer.get(offset + 1) & 0xFF) << 8);
  }

  private int read32BitsFrom(@IntRange(from = 0) int offset) {
    return (mappedByteBuffer.get(offset) & 0xFF)
        | ((mappedByteBuffer.get(offset + 1) & 0xFF) << 8)
        | ((mappedByteBuffer.get(offset + 2) & 0xFF) << 16)
        | ((mappedByteBuffer.get(offset + 3) & 0xFF) << 24);
  }

  @NonNull
  private String readLocaleFrom(@IntRange(from = 0) int offset) {
    final int length;
    mappedByteBuffer.position(offset);
    int localePosition = mappedByteBuffer.position();
    if (mappedByteBuffer.get(localePosition + 2) == '\0') {
      length = 2;
    } else if (mappedByteBuffer.get(localePosition + 5) == '\0') {
      length = 5;
    } else {
      length = StringPackData.LOCALE_CODE_SIZE;
    }
    byte[] stringBytes = new byte[length];
    mappedByteBuffer.get(stringBytes, 0, stringBytes.length);
    return new String(stringBytes, 0, stringBytes.length, StringPackData.ASCII);
  }

  /**
   * Maps the id -> location for strings and plurals.
   * @param startOfLocaleData
   * @param headerStart
   */
  private void mapTranslations(@IntRange(from = 0) int startOfLocaleData, int headerStart) {
    int caret = startOfLocaleData + headerStart;
    final int numStrings = read16BitsFrom(caret);
    caret += 2; // Increment by 2 Bytes which is the number of strings read above
    final int numPlurals = read16BitsFrom(caret);
    caret += 2; // Increment by 2 Bytes which is number of plurals read above

    for (int i = 0; i < numStrings; i++) {
      final int id = read16BitsFrom(caret);
      caret += 2; // Increment by 2 Bytes which is the id of the string read above
      stringSparseArray.append(id, caret);
      caret += 6; // Increment by 6 Bytes which is string starting location (4) + string length (2)
                  // to be read later when string is fetched
    }

    for (int i = 0; i < numPlurals; i++) {
      final int id = read16BitsFrom(caret);
      caret += 2; // Increment by 2 Bytes which is the id of the plural read above
      pluralSparseArray.append(id, caret);
      final int quantityCount = mappedByteBuffer.get(caret);
      caret++; // Increment by a single byte which are for quantity count read above
      for (int j = 0; j < quantityCount; j++) {
        caret += 7; // Increment by 7 Bytes which is the
                    // quantity id (1) + string starting location (4) + string length (2) to be
                    // read later when plural is fetched
      }
    }
  }

  public synchronized String loadString(int id) {
    int position = stringSparseArray.get(id);
    if (position == 0) {
      return null;
    }
    mappedByteBuffer.position(stringSparseArray.get(id));
    int caret = mappedByteBuffer.position();
    final int stringStart = read32BitsFrom(caret);
    caret += 4; //Increment to 4 Bytes which we read above for string starting location
    final int stringLen = read16BitsFrom(caret);
    byte[] stringBytes = new byte[stringLen];
    mappedByteBuffer.position(startOfStringData + stringStart);
    mappedByteBuffer.get(stringBytes,0, stringBytes.length);
    return new String(stringBytes, encoding);
  }

  public synchronized String[] loadPlural(int id) {
    mappedByteBuffer.position(pluralSparseArray.get(id));
    int caret = mappedByteBuffer.position();
    final int quantityCount = mappedByteBuffer.get(caret);
    caret++; // Increment by a single byte which are for quantity count
    final String[] pluralMMap = new String[6];
    for (int j = 0; j < quantityCount; j++) {
      final int quantityId = mappedByteBuffer.get(caret);
      caret++; // Increment by a single byte which are for quantity id
      final int stringStart = read32BitsFrom(caret);
      caret += 4; // Increment to 4*8 Bits which we read above for plural starting location
      final int stringLen = read16BitsFrom(caret);
      caret += 2; // Increment to 2*8 Bits which we read above for plural length
      byte[] stringBytes = new byte[stringLen];
      mappedByteBuffer.position(startOfStringData + stringStart);
      mappedByteBuffer.get(stringBytes,0, stringBytes.length);
      pluralMMap[quantityId] = new String(stringBytes, 0, stringLen, encoding);
    }
    return pluralMMap;
  }

  public boolean isEmpty() {
    return stringSparseArray.size() == 0;
  }

}
