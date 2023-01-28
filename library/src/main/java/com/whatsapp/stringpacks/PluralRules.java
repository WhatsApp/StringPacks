/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.whatsapp.stringpacks;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Size;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * This class is needed in WhatsApp to work around the incorrect or missing plural rules in earlier
 * versions of Android, as well as support for non-int quantity selectors.
 */
public abstract class PluralRules {
  public static final int QUANTITY_OTHER = 0x0000;
  public static final int QUANTITY_ZERO = 0x0001;
  public static final int QUANTITY_ONE = 0x0002;
  public static final int QUANTITY_TWO = 0x0004;
  public static final int QUANTITY_FEW = 0x0008;
  public static final int QUANTITY_MANY = 0x0010;

  @Retention(RetentionPolicy.SOURCE)
  @IntDef({QUANTITY_OTHER, QUANTITY_ZERO, QUANTITY_ONE, QUANTITY_TWO, QUANTITY_FEW, QUANTITY_MANY})
  public @interface Quantity {}

  /**
   * For definition and examples for the parameters, @see <a
   * href="http://unicode.org/reports/tr35/tr35-numbers.html#Plural_Operand_Meanings">the LDML
   * spec</a>.
   *
   * @param n absolute value of the source number (integer and decimals)
   * @param i integer digits of n
   * @param v number of visible fraction digits in n, <i>with</i> trailing zeros.
   * @param w number of visible fraction digits in n, <i>without</i> trailing zeros.
   * @param f visible fractional digits in n, <i>with</i> trailing zeros.
   * @param t visible fractional digits in n, <i>without</i> trailing zeros.
   */
  @Quantity
  public abstract int quantityForNumber(double n, long i, int v, int w, long f, long t);

  @Quantity
  public int quantityForNumber(long number) {
    return quantityForNumber(number, number, 0, 0, 0, 0);
  }

  /**
   * Takes a decimal number as a string, and returns a quantity selector based on it. The input
   * number should be formatted with US English rules with no thousands separators, such as
   * "123456", "7", "5.10", "1.5", or "0.1234", otherwise the result in undefined.
   */
  @Quantity
  public int quantityForNumber(@NonNull @Size(min = 1) String number) {
    final int decimalIndex = number.indexOf('.');
    try {
      if (decimalIndex == -1) {
        final long i = Long.parseLong(number);
        return quantityForNumber(i, i, 0, 0, 0, 0);
      } else {
        final double n = Double.parseDouble(number);
        final long i = decimalIndex == 0 ? 0 : Long.parseLong(number.substring(0, decimalIndex));

        final String fractional = number.substring(decimalIndex + 1);
        final int v = fractional.length();
        int w = v;
        while (w > 0 && fractional.charAt(w - 1) == '0') {
          w--;
        }
        final long f = Long.parseLong(fractional);
        final long t = w == 0 ? 0 : Long.parseLong(fractional.substring(0, w));
        return quantityForNumber(n, i, v, w, f, t);
      }
    } catch (NumberFormatException e) {
      return QUANTITY_OTHER;
    }
  }

  private static Map<String, PluralRules> allRules = new HashMap<>();

  // The following rules are based on CLDR plurals information, specifically
  // http://unicode.org/cldr/trac/browser/trunk/common/supplemental/plurals.xml?rev=14397
  // Copyright © 1991-2015 Unicode, Inc.
  // For terms of use, see http://www.unicode.org/copyright.html
  static {
    addRules(
        "bm bo dz id ig ii in ja jbo jv jw kde kea km ko lkt lo ms my nqo osa root sah ses sg su th to vi wo yo yue zh",
        new PluralRules() {
          @Quantity
          public int quantityForNumber(double n, long i, int v, int w, long f, long t) {
            return QUANTITY_OTHER;
          }
        });
    addRules(
        "am as bn fa gu hi kn zu",
        new PluralRules() {
          @Quantity
          public int quantityForNumber(double n, long i, int v, int w, long f, long t) {
            /* i = 0 or n = 1 */
            if (i == 0 || n == 1) {
              return QUANTITY_ONE;
            }
            return QUANTITY_OTHER;
          }
        });
    addRules(
        "ff fr hy kab",
        new PluralRules() {
          @Quantity
          public int quantityForNumber(double n, long i, int v, int w, long f, long t) {
            /* i = 0,1 */
            if (i == 0 || i == 1) {
              return QUANTITY_ONE;
            }
            return QUANTITY_OTHER;
          }
        });
    addRule(
        "pt",
        new PluralRules() {
          @Quantity
          public int quantityForNumber(double n, long i, int v, int w, long f, long t) {
            /* i = 0..1 */
            if (0 <= i && i <= 1) {
              return QUANTITY_ONE;
            }
            return QUANTITY_OTHER;
          }
        });
    addRules(
        "ast ca de en et fi fy gl ia io it ji nl pt_PT sc scn sv sw ur yi",
        new PluralRules() {
          @Quantity
          public int quantityForNumber(double n, long i, int v, int w, long f, long t) {
            /* i = 1 and v = 0 */
            if ((i == 1) && (v == 0)) {
              return QUANTITY_ONE;
            }
            return QUANTITY_OTHER;
          }
        });
    addRule(
        "si",
        new PluralRules() {
          @Quantity
          public int quantityForNumber(double n, long i, int v, int w, long f, long t) {
            /* n = 0,1 or i = 0 and f = 1 */
            if (n == 0 || n == 1 || (i == 0) && (f == 1)) {
              return QUANTITY_ONE;
            }
            return QUANTITY_OTHER;
          }
        });
    addRules(
        "ak bho guw ln mg nso pa ti wa",
        new PluralRules() {
          @Quantity
          public int quantityForNumber(double n, long i, int v, int w, long f, long t) {
            /* n = 0..1 */
            if (n == i && 0 <= n && n <= 1) {
              return QUANTITY_ONE;
            }
            return QUANTITY_OTHER;
          }
        });
    addRule(
        "tzm",
        new PluralRules() {
          @Quantity
          public int quantityForNumber(double n, long i, int v, int w, long f, long t) {
            /* n = 0..1 or n = 11..99 */
            if (n == i && 0 <= n && n <= 1 || n == i && 11 <= n && n <= 99) {
              return QUANTITY_ONE;
            }
            return QUANTITY_OTHER;
          }
        });
    addRules(
        "af an asa az bem bez bg brx ce cgg chr ckb dv ee el eo es eu fo fur gsw ha haw hu jgo jmc ka kaj kcg kk kkj kl ks ksb ku ky lb lg mas mgo ml mn mr nah nb nd ne nn nnh no nr ny nyn om or os pap ps rm rof rwk saq sd sdh seh sn so sq ss ssy st syr ta te teo tig tk tn tr ts ug uz ve vo vun wae xh xog",
        new PluralRules() {
          @Quantity
          public int quantityForNumber(double n, long i, int v, int w, long f, long t) {
            /* n = 1 */
            if (n == 1) {
              return QUANTITY_ONE;
            }
            return QUANTITY_OTHER;
          }
        });
    addRule(
        "da",
        new PluralRules() {
          @Quantity
          public int quantityForNumber(double n, long i, int v, int w, long f, long t) {
            /* n = 1 or t != 0 and i = 0,1 */
            if (n == 1 || (t != 0) && (i == 0 || i == 1)) {
              return QUANTITY_ONE;
            }
            return QUANTITY_OTHER;
          }
        });
    addRule(
        "is",
        new PluralRules() {
          @Quantity
          public int quantityForNumber(double n, long i, int v, int w, long f, long t) {
            /* t = 0 and i % 10 = 1 and i % 100 != 11 or t != 0 */
            if ((t == 0) && ((i % 10) == 1) && ((i % 100) != 11) || t != 0) {
              return QUANTITY_ONE;
            }
            return QUANTITY_OTHER;
          }
        });
    addRule(
        "mk",
        new PluralRules() {
          @Quantity
          public int quantityForNumber(double n, long i, int v, int w, long f, long t) {
            /* v = 0 and i % 10 = 1 and i % 100 != 11 or f % 10 = 1 and f % 100 != 11 */
            if ((v == 0) && ((i % 10) == 1) && ((i % 100) != 11)
                || ((f % 10) == 1) && ((f % 100) != 11)) {
              return QUANTITY_ONE;
            }
            return QUANTITY_OTHER;
          }
        });
    addRules(
        "ceb fil tl",
        new PluralRules() {
          @Quantity
          public int quantityForNumber(double n, long i, int v, int w, long f, long t) {
            /* v = 0 and i = 1,2,3 or v = 0 and i % 10 != 4,6,9 or v != 0 and f % 10 != 4,6,9 */
            if ((v == 0) && (i == 1 || i == 2 || i == 3)
                || (v == 0) && ((i % 10) != 4 && (i % 10) != 6 && (i % 10) != 9)
                || (v != 0) && ((f % 10) != 4 && (f % 10) != 6 && (f % 10) != 9)) {
              return QUANTITY_ONE;
            }
            return QUANTITY_OTHER;
          }
        });
    addRules(
        "lv prg",
        new PluralRules() {
          @Quantity
          public int quantityForNumber(double n, long i, int v, int w, long f, long t) {
            /* n % 10 = 0 or n % 100 = 11..19 or v = 2 and f % 100 = 11..19 */
            if ((n % 10) == 0
                || n == i && 11 <= (n % 100) && (n % 100) <= 19
                || (v == 2) && (11 <= (f % 100) && (f % 100) <= 19)) {
              return QUANTITY_ZERO;
            }
            /* n % 10 = 1 and n % 100 != 11 or v = 2 and f % 10 = 1 and f % 100 != 11 or v != 2 and f % 10 = 1 */
            if (((n % 10) == 1) && ((n % 100) != 11)
                || (v == 2) && ((f % 10) == 1) && ((f % 100) != 11)
                || (v != 2) && ((f % 10) == 1)) {
              return QUANTITY_ONE;
            }
            return QUANTITY_OTHER;
          }
        });
    addRule(
        "lag",
        new PluralRules() {
          @Quantity
          public int quantityForNumber(double n, long i, int v, int w, long f, long t) {
            /* n = 0 */
            if (n == 0) {
              return QUANTITY_ZERO;
            }
            /* i = 0,1 and n != 0 */
            if ((i == 0 || i == 1) && (n != 0)) {
              return QUANTITY_ONE;
            }
            return QUANTITY_OTHER;
          }
        });
    addRule(
        "ksh",
        new PluralRules() {
          @Quantity
          public int quantityForNumber(double n, long i, int v, int w, long f, long t) {
            /* n = 0 */
            if (n == 0) {
              return QUANTITY_ZERO;
            }
            /* n = 1 */
            if (n == 1) {
              return QUANTITY_ONE;
            }
            return QUANTITY_OTHER;
          }
        });
    addRules(
        "iu naq se sma smi smj smn sms",
        new PluralRules() {
          @Quantity
          public int quantityForNumber(double n, long i, int v, int w, long f, long t) {
            /* n = 1 */
            if (n == 1) {
              return QUANTITY_ONE;
            }
            /* n = 2 */
            if (n == 2) {
              return QUANTITY_TWO;
            }
            return QUANTITY_OTHER;
          }
        });
    addRule(
        "shi",
        new PluralRules() {
          @Quantity
          public int quantityForNumber(double n, long i, int v, int w, long f, long t) {
            /* i = 0 or n = 1 */
            if (i == 0 || n == 1) {
              return QUANTITY_ONE;
            }
            /* n = 2..10 */
            if (n == i && 2 <= n && n <= 10) {
              return QUANTITY_FEW;
            }
            return QUANTITY_OTHER;
          }
        });
    addRules(
        "mo ro",
        new PluralRules() {
          @Quantity
          public int quantityForNumber(double n, long i, int v, int w, long f, long t) {
            /* i = 1 and v = 0 */
            if ((i == 1) && (v == 0)) {
              return QUANTITY_ONE;
            }
            /* v != 0 or n = 0 or n % 100 = 2..19 */
            if (v != 0 || n == 0 || n == i && 2 <= (n % 100) && (n % 100) <= 19) {
              return QUANTITY_FEW;
            }
            return QUANTITY_OTHER;
          }
        });
    addRules(
        "bs hr sh sr",
        new PluralRules() {
          @Quantity
          public int quantityForNumber(double n, long i, int v, int w, long f, long t) {
            /* v = 0 and i % 10 = 1 and i % 100 != 11 or f % 10 = 1 and f % 100 != 11 */
            if ((v == 0) && ((i % 10) == 1) && ((i % 100) != 11)
                || ((f % 10) == 1) && ((f % 100) != 11)) {
              return QUANTITY_ONE;
            }
            /* v = 0 and i % 10 = 2..4 and i % 100 != 12..14 or f % 10 = 2..4 and f % 100 != 12..14 */
            if ((v == 0) && (2 <= (i % 10) && (i % 10) <= 4) && (12 > (i % 100) || (i % 100) > 14)
                || (2 <= (f % 10) && (f % 10) <= 4) && (12 > (f % 100) || (f % 100) > 14)) {
              return QUANTITY_FEW;
            }
            return QUANTITY_OTHER;
          }
        });
    addRule(
        "gd",
        new PluralRules() {
          @Quantity
          public int quantityForNumber(double n, long i, int v, int w, long f, long t) {
            /* n = 1,11 */
            if (n == 1 || n == 11) {
              return QUANTITY_ONE;
            }
            /* n = 2,12 */
            if (n == 2 || n == 12) {
              return QUANTITY_TWO;
            }
            /* n = 3..10,13..19 */
            if (n == i && 3 <= n && n <= 10 || n == i && 13 <= n && n <= 19) {
              return QUANTITY_FEW;
            }
            return QUANTITY_OTHER;
          }
        });
    addRule(
        "sl",
        new PluralRules() {
          @Quantity
          public int quantityForNumber(double n, long i, int v, int w, long f, long t) {
            /* v = 0 and i % 100 = 1 */
            if ((v == 0) && ((i % 100) == 1)) {
              return QUANTITY_ONE;
            }
            /* v = 0 and i % 100 = 2 */
            if ((v == 0) && ((i % 100) == 2)) {
              return QUANTITY_TWO;
            }
            /* v = 0 and i % 100 = 3..4 or v != 0 */
            if ((v == 0) && (3 <= (i % 100) && (i % 100) <= 4) || v != 0) {
              return QUANTITY_FEW;
            }
            return QUANTITY_OTHER;
          }
        });
    addRules(
        "dsb hsb",
        new PluralRules() {
          @Quantity
          public int quantityForNumber(double n, long i, int v, int w, long f, long t) {
            /* v = 0 and i % 100 = 1 or f % 100 = 1 */
            if ((v == 0) && ((i % 100) == 1) || (f % 100) == 1) {
              return QUANTITY_ONE;
            }
            /* v = 0 and i % 100 = 2 or f % 100 = 2 */
            if ((v == 0) && ((i % 100) == 2) || (f % 100) == 2) {
              return QUANTITY_TWO;
            }
            /* v = 0 and i % 100 = 3..4 or f % 100 = 3..4 */
            if ((v == 0) && (3 <= (i % 100) && (i % 100) <= 4)
                || 3 <= (f % 100) && (f % 100) <= 4) {
              return QUANTITY_FEW;
            }
            return QUANTITY_OTHER;
          }
        });
    addRules(
        "he iw",
        new PluralRules() {
          @Quantity
          public int quantityForNumber(double n, long i, int v, int w, long f, long t) {
            /* i = 1 and v = 0 */
            if ((i == 1) && (v == 0)) {
              return QUANTITY_ONE;
            }
            /* i = 2 and v = 0 */
            if ((i == 2) && (v == 0)) {
              return QUANTITY_TWO;
            }
            /* v = 0 and n != 0..10 and n % 10 = 0 */
            if ((v == 0) && (n != i || 0 > n || n > 10) && ((n % 10) == 0)) {
              return QUANTITY_MANY;
            }
            return QUANTITY_OTHER;
          }
        });
    addRules(
        "cs sk",
        new PluralRules() {
          @Quantity
          public int quantityForNumber(double n, long i, int v, int w, long f, long t) {
            /* i = 1 and v = 0 */
            if ((i == 1) && (v == 0)) {
              return QUANTITY_ONE;
            }
            /* i = 2..4 and v = 0 */
            if ((2 <= i && i <= 4) && (v == 0)) {
              return QUANTITY_FEW;
            }
            /* v != 0 */
            if (v != 0) {
              return QUANTITY_MANY;
            }
            return QUANTITY_OTHER;
          }
        });
    addRule(
        "pl",
        new PluralRules() {
          @Quantity
          public int quantityForNumber(double n, long i, int v, int w, long f, long t) {
            /* i = 1 and v = 0 */
            if ((i == 1) && (v == 0)) {
              return QUANTITY_ONE;
            }
            /* v = 0 and i % 10 = 2..4 and i % 100 != 12..14 */
            if ((v == 0)
                && (2 <= (i % 10) && (i % 10) <= 4)
                && (12 > (i % 100) || (i % 100) > 14)) {
              return QUANTITY_FEW;
            }
            /* v = 0 and i != 1 and i % 10 = 0..1 or v = 0 and i % 10 = 5..9 or v = 0 and i % 100 = 12..14 */
            if ((v == 0) && (i != 1) && (0 <= (i % 10) && (i % 10) <= 1)
                || (v == 0) && (5 <= (i % 10) && (i % 10) <= 9)
                || (v == 0) && (12 <= (i % 100) && (i % 100) <= 14)) {
              return QUANTITY_MANY;
            }
            return QUANTITY_OTHER;
          }
        });
    addRule(
        "be",
        new PluralRules() {
          @Quantity
          public int quantityForNumber(double n, long i, int v, int w, long f, long t) {
            /* n % 10 = 1 and n % 100 != 11 */
            if (((n % 10) == 1) && ((n % 100) != 11)) {
              return QUANTITY_ONE;
            }
            /* n % 10 = 2..4 and n % 100 != 12..14 */
            if ((n == i && 2 <= (n % 10) && (n % 10) <= 4)
                && (n != i || 12 > (n % 100) || (n % 100) > 14)) {
              return QUANTITY_FEW;
            }
            /* n % 10 = 0 or n % 10 = 5..9 or n % 100 = 11..14 */
            if ((n % 10) == 0
                || n == i && 5 <= (n % 10) && (n % 10) <= 9
                || n == i && 11 <= (n % 100) && (n % 100) <= 14) {
              return QUANTITY_MANY;
            }
            return QUANTITY_OTHER;
          }
        });
    addRule(
        "lt",
        new PluralRules() {
          @Quantity
          public int quantityForNumber(double n, long i, int v, int w, long f, long t) {
            /* n % 10 = 1 and n % 100 != 11..19 */
            if (((n % 10) == 1) && (n != i || 11 > (n % 100) || (n % 100) > 19)) {
              return QUANTITY_ONE;
            }
            /* n % 10 = 2..9 and n % 100 != 11..19 */
            if ((n == i && 2 <= (n % 10) && (n % 10) <= 9)
                && (n != i || 11 > (n % 100) || (n % 100) > 19)) {
              return QUANTITY_FEW;
            }
            /* f != 0 */
            if (f != 0) {
              return QUANTITY_MANY;
            }
            return QUANTITY_OTHER;
          }
        });
    addRule(
        "mt",
        new PluralRules() {
          @Quantity
          public int quantityForNumber(double n, long i, int v, int w, long f, long t) {
            /* n = 1 */
            if (n == 1) {
              return QUANTITY_ONE;
            }
            /* n = 0 or n % 100 = 2..10 */
            if (n == 0 || n == i && 2 <= (n % 100) && (n % 100) <= 10) {
              return QUANTITY_FEW;
            }
            /* n % 100 = 11..19 */
            if (n == i && 11 <= (n % 100) && (n % 100) <= 19) {
              return QUANTITY_MANY;
            }
            return QUANTITY_OTHER;
          }
        });
    addRules(
        "ru uk",
        new PluralRules() {
          @Quantity
          public int quantityForNumber(double n, long i, int v, int w, long f, long t) {
            /* v = 0 and i % 10 = 1 and i % 100 != 11 */
            if ((v == 0) && ((i % 10) == 1) && ((i % 100) != 11)) {
              return QUANTITY_ONE;
            }
            /* v = 0 and i % 10 = 2..4 and i % 100 != 12..14 */
            if ((v == 0)
                && (2 <= (i % 10) && (i % 10) <= 4)
                && (12 > (i % 100) || (i % 100) > 14)) {
              return QUANTITY_FEW;
            }
            /* v = 0 and i % 10 = 0 or v = 0 and i % 10 = 5..9 or v = 0 and i % 100 = 11..14 */
            if ((v == 0) && ((i % 10) == 0)
                || (v == 0) && (5 <= (i % 10) && (i % 10) <= 9)
                || (v == 0) && (11 <= (i % 100) && (i % 100) <= 14)) {
              return QUANTITY_MANY;
            }
            return QUANTITY_OTHER;
          }
        });
    addRule(
        "br",
        new PluralRules() {
          @Quantity
          public int quantityForNumber(double n, long i, int v, int w, long f, long t) {
            /* n % 10 = 1 and n % 100 != 11,71,91 */
            if (((n % 10) == 1) && ((n % 100) != 11 && (n % 100) != 71 && (n % 100) != 91)) {
              return QUANTITY_ONE;
            }
            /* n % 10 = 2 and n % 100 != 12,72,92 */
            if (((n % 10) == 2) && ((n % 100) != 12 && (n % 100) != 72 && (n % 100) != 92)) {
              return QUANTITY_TWO;
            }
            /* n % 10 = 3..4,9 and n % 100 != 10..19,70..79,90..99 */
            if ((n == i && 3 <= (n % 10) && (n % 10) <= 4 || (n % 10) == 9)
                && (n != i
                    || 10 > (n % 100)
                    || (n % 100) > 19 && n != i
                    || 70 > (n % 100)
                    || (n % 100) > 79 && n != i
                    || 90 > (n % 100)
                    || (n % 100) > 99)) {
              return QUANTITY_FEW;
            }
            /* n != 0 and n % 1000000 = 0 */
            if ((n != 0) && ((n % 1000000) == 0)) {
              return QUANTITY_MANY;
            }
            return QUANTITY_OTHER;
          }
        });
    addRule(
        "ga",
        new PluralRules() {
          @Quantity
          public int quantityForNumber(double n, long i, int v, int w, long f, long t) {
            /* n = 1 */
            if (n == 1) {
              return QUANTITY_ONE;
            }
            /* n = 2 */
            if (n == 2) {
              return QUANTITY_TWO;
            }
            /* n = 3..6 */
            if (n == i && 3 <= n && n <= 6) {
              return QUANTITY_FEW;
            }
            /* n = 7..10 */
            if (n == i && 7 <= n && n <= 10) {
              return QUANTITY_MANY;
            }
            return QUANTITY_OTHER;
          }
        });
    addRule(
        "gv",
        new PluralRules() {
          @Quantity
          public int quantityForNumber(double n, long i, int v, int w, long f, long t) {
            /* v = 0 and i % 10 = 1 */
            if ((v == 0) && ((i % 10) == 1)) {
              return QUANTITY_ONE;
            }
            /* v = 0 and i % 10 = 2 */
            if ((v == 0) && ((i % 10) == 2)) {
              return QUANTITY_TWO;
            }
            /* v = 0 and i % 100 = 0,20,40,60,80 */
            if ((v == 0)
                && ((i % 100) == 0
                    || (i % 100) == 20
                    || (i % 100) == 40
                    || (i % 100) == 60
                    || (i % 100) == 80)) {
              return QUANTITY_FEW;
            }
            /* v != 0 */
            if (v != 0) {
              return QUANTITY_MANY;
            }
            return QUANTITY_OTHER;
          }
        });
    addRule(
        "kw",
        new PluralRules() {
          @Quantity
          public int quantityForNumber(double n, long i, int v, int w, long f, long t) {
            /* n = 0 */
            if (n == 0) {
              return QUANTITY_ZERO;
            }
            /* n = 1 */
            if (n == 1) {
              return QUANTITY_ONE;
            }
            /* n % 100 = 2,22,42,62,82 or n % 1000 = 0 and n % 100000 = 1000..20000,40000,60000,80000 or n != 0 and n % 1000000 = 100000 */
            if ((n % 100) == 2
                || (n % 100) == 22
                || (n % 100) == 42
                || (n % 100) == 62
                || (n % 100) == 82
                || ((n % 1000) == 0)
                    && (n == i && 1000 <= (n % 100000) && (n % 100000) <= 20000
                        || (n % 100000) == 40000
                        || (n % 100000) == 60000
                        || (n % 100000) == 80000)
                || (n != 0) && ((n % 1000000) == 100000)) {
              return QUANTITY_TWO;
            }
            /* n % 100 = 3,23,43,63,83 */
            if ((n % 100) == 3
                || (n % 100) == 23
                || (n % 100) == 43
                || (n % 100) == 63
                || (n % 100) == 83) {
              return QUANTITY_FEW;
            }
            /* n != 1 and n % 100 = 1,21,41,61,81 */
            if ((n != 1)
                && ((n % 100) == 1
                    || (n % 100) == 21
                    || (n % 100) == 41
                    || (n % 100) == 61
                    || (n % 100) == 81)) {
              return QUANTITY_MANY;
            }
            return QUANTITY_OTHER;
          }
        });
    addRules(
        "ar ars",
        new PluralRules() {
          @Quantity
          public int quantityForNumber(double n, long i, int v, int w, long f, long t) {
            /* n = 0 */
            if (n == 0) {
              return QUANTITY_ZERO;
            }
            /* n = 1 */
            if (n == 1) {
              return QUANTITY_ONE;
            }
            /* n = 2 */
            if (n == 2) {
              return QUANTITY_TWO;
            }
            /* n % 100 = 3..10 */
            if (n == i && 3 <= (n % 100) && (n % 100) <= 10) {
              return QUANTITY_FEW;
            }
            /* n % 100 = 11..99 */
            if (n == i && 11 <= (n % 100) && (n % 100) <= 99) {
              return QUANTITY_MANY;
            }
            return QUANTITY_OTHER;
          }
        });
    addRule(
        "cy",
        new PluralRules() {
          @Quantity
          public int quantityForNumber(double n, long i, int v, int w, long f, long t) {
            /* n = 0 */
            if (n == 0) {
              return QUANTITY_ZERO;
            }
            /* n = 1 */
            if (n == 1) {
              return QUANTITY_ONE;
            }
            /* n = 2 */
            if (n == 2) {
              return QUANTITY_TWO;
            }
            /* n = 3 */
            if (n == 3) {
              return QUANTITY_FEW;
            }
            /* n = 6 */
            if (n == 6) {
              return QUANTITY_MANY;
            }
            return QUANTITY_OTHER;
          }
        });
  }

  private static void addRules(@NonNull String languages, @NonNull PluralRules rules) {
    for (String language : languages.split(" ")) {
      addRule(language, rules);
    }
  }

  private static void addRule(@NonNull String language, @NonNull PluralRules rules) {
    allRules.put(language, rules);
    if ("pt_PT".equals(language)) {
      // If we are on European Portuguese, add all its sublocales.
      for (String region : StringPackData.EUROPEAN_PORTUGUESE_LOCALES) {
        allRules.put("pt_" + region, rules);
      }
    }
  }

  @NonNull
  public static PluralRules ruleForLocale(@NonNull Locale locale) {
    final String language = locale.getLanguage();
    final String region = locale.getCountry();
    PluralRules rules;
    if (!region.isEmpty()) {
      // Try both language and region first, if there's a region (Portuguese rules differ based on
      // region).
      rules = allRules.get(language + "_" + region);
      if (rules != null) {
        return rules;
      }
    }
    // Now try just the language.
    rules = allRules.get(language);
    if (rules != null) {
      return rules;
    }
    // Still not found. Return the default rule, which is an "other"-only rule.
    rules = allRules.get("root");
    if (rules != null) {
      return rules;
    }
    // If root doesn't exist, we are broken beyond repair.
    throw new NullPointerException("No plural rule found for 'root' locale.");
  }
}
