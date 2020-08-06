/* Copyright (c) Facebook, Inc. and its affiliates. All rights reserved.
 *
 * This source code is licensed under the Apache 2.0 license found in
 * the LICENSE file in the root directory of this source tree.
 */

package com.whatsapp.stringpacks;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import java.io.InputStream;
import java.nio.MappedByteBuffer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;

public class ParsedStringPack {

  @NonNull
  private final ConcurrentHashMap<Integer, String> strings = new ConcurrentHashMap<>();
  @NonNull
  private final ConcurrentHashMap<Integer, String[]> plurals = new ConcurrentHashMap<>();

  @NonNull
  private final LoadedStringPack loadedStringPack;
  @Nullable
  private MMappedStringPack mMappedStringPack;

  public ParsedStringPack(
      @NonNull InputStream inputStream,
      @NonNull List<String> parentLocales,
      @Nullable MappedByteBuffer mappedPackFile) {
    loadedStringPack = new LoadedStringPack(inputStream, parentLocales);
    if (mappedPackFile != null) {
      mMappedStringPack = new MMappedStringPack(parentLocales, mappedPackFile);
    }
  }

  public boolean isEmpty() {
    if (mMappedStringPack != null) {
      return mMappedStringPack.isEmpty();
    }
    return loadedStringPack.isEmpty();
  }

  @Nullable
  public String getString(int id, final boolean readFromMMap) {
    final String result = strings.get(id);
    if (result != null) {
      return result;
    }
      // String not loaded or doesn't exist.
    String loadedString = null;
    if (readFromMMap && mMappedStringPack != null) {
        loadedString = mMappedStringPack.loadString(id);
    } else { 
      loadedString = loadedStringPack.loadString(id);
    }
    if (loadedString != null) {
      strings.put(id, loadedString);
    }
    return loadedString;
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
  public String getQuantityString(int id, Object quantity, @NonNull PluralRules pluralRules, final boolean readFromMMap) {
    String[] plural = plurals.get(id);
    if (plural == null) {
      // Plural set not loaded or doesn't exist.
      String[] loadedPlural = null;
      if (readFromMMap && mMappedStringPack != null) {
        loadedPlural = mMappedStringPack.loadPlural(id);
      } else {
        loadedPlural = loadedStringPack.loadPlural(id);
      }
      if (loadedPlural != null) {
        plurals.put(id, loadedPlural);
      }
      plural = loadedPlural;
    }
    if (plural == null) {
      // It doesn't exist.
      return null;
    }
    // TODO: pluralRules only accept Strings or Longs, we need to convert `quantity` type if needed.
    final int index = quantityIndex(pluralRules.quantityForNumber(quantity));
    String result = plural[index];
    if (result != null) {
      return result;
    }
    // Fallback to QUANTITY_OTHER.
    return plural[0];
  }
}
