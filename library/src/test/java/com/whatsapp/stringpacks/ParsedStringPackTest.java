/* Copyright (c) Facebook, Inc. and its affiliates. All rights reserved.
 *
 * This source code is licensed under the Apache 2.0 license found in
 * the LICENSE file in the root directory of this source tree.
 */

package com.whatsapp.stringpacks;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.InputStream;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(RobolectricTestRunner.class)
public class ParsedStringPackTest {

  private static final PluralRules TEST_PLURAL_RULES =
      new PluralRules() {
        @PluralRules.Quantity
        public int quantityForNumber(double n, long i, int v, int w, long f, long t) {
          if (n == 0) {
            return QUANTITY_ZERO;
          }
          if (n == 1) {
            return QUANTITY_ONE;
          }
          if (n == 2) {
            return QUANTITY_TWO;
          }
          if (n == 3) {
            return QUANTITY_FEW;
          }
          if (n == 4) {
            return QUANTITY_MANY;
          }
          return QUANTITY_OTHER;
        }
      };

  private static int PLURALS_ID = 0;
  private static int STRING_ID = 1;

  private ParsedStringPack parsedStringPack;

  @Before
  public void setUp() {
    InputStream inputStream = getClass().getClassLoader().getResourceAsStream("strings_zh.pack");
    parsedStringPack = new ParsedStringPack(inputStream, Collections.singletonList("zh"));
  }

  @Test
  public void getString() {
    String string = parsedStringPack.getString(STRING_ID);
    assertEquals("你好，世界", string);
  }

  @Test
  public void getString_MultipleTimes() {
    String first = parsedStringPack.getString(STRING_ID);
    assertEquals("你好，世界", first);

    String second = parsedStringPack.getString(STRING_ID);
    assertEquals("你好，世界", second);
  }

  @Test
  public void getString_WithNonexistentId() {
    String nonexistent = parsedStringPack.getString(2);
    assertNull(nonexistent);
  }

  @Test
  public void getPlurals_WithTestPluralRules() {
    String[] expectedQuantityStrings = {"零个", "一个", "两个", "少许", "多数", "其他"};
    for (int i = 0; i < expectedQuantityStrings.length; i++) {
      String string = parsedStringPack.getQuantityString(PLURALS_ID, (long) i, TEST_PLURAL_RULES);
      assertEquals(expectedQuantityStrings[i], string);
    }
  }
}
