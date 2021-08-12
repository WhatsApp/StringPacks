/* Copyright (c) Facebook, Inc. and its affiliates. All rights reserved.
 *
 * This source code is licensed under the Apache 2.0 license found in
 * the LICENSE file in the root directory of this source tree.
 */

package com.whatsapp.stringpacks;

public class StringPacksTestData {
  static final PluralRules TEST_PLURAL_RULES =
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

  static int PLURALS_ID = 0;
  static int STRING_ID = 15;
  static int FALLBACK_STRING_ID = 11;
  static String[] EXPECTED_STRINGS = {
    null, "你好非洲", "南极洲你好", "你好北冰洋", "你好亚洲", "你好大西洋", "澳大利亚你好", "您好欧洲", "你好森林", "你好印度洋", "你好山",
    "你好，北美", "你好太平洋", "你好河", "您好，南美", "你好，世界"
  };
}
