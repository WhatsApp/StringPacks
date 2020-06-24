/* Copyright (c) Facebook, Inc. and its affiliates. All rights reserved.
 *
 * This source code is licensed under the Apache 2.0 license found in
 * the LICENSE file in the root directory of this source tree.
 */

package com.whatsapp.stringpacks;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.InputStream;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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
  private static int STRING_ID = 15;
  private static String[] EXPECTED_STRINGS = {null, "你好非洲", "南极洲你好", "你好北冰洋", "你好亚洲", "你好大西洋", "澳大利亚你好", "您好欧洲", "你好森林", "你好印度洋", "你好山", "你好，北美", "你好太平洋", "你好河", "您好，南美", "你好，世界"};

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
    String nonexistent = parsedStringPack.getString(EXPECTED_STRINGS.length + 1);
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

  @Test
  public void getString_onDemandLoadingSameString_calledFromMultipleThreads() throws InterruptedException {
    int numberOfThreads = 100;
    String expected = "你好，世界";
    ExecutorService service = Executors.newFixedThreadPool(numberOfThreads);
    CountDownLatch latch = new CountDownLatch(numberOfThreads);

    Callable<String> getStringTask = () -> {
      String string = parsedStringPack.getString(STRING_ID);
      latch.countDown();
      return string;
    };
    List<Callable<String>> tasks = new ArrayList<>();
    for (int i = 0; i < numberOfThreads; i++) {
      tasks.add(getStringTask);
    }
    List<Future<String>> results = service.invokeAll(tasks, 2, TimeUnit.SECONDS);
    latch.await();
    // To allow any remaining tasks to return their result since latch.countDown happened before
    // returning the string
    Thread.sleep(2000);
    for (int i = 0; i < numberOfThreads; i++) {
      Future<String> result = results.get(i);
      assertTrue(!result.isCancelled());
      try {
        assertEquals("Problem fetching correct string", expected, result.get());
      } catch (ExecutionException ee) {
        assertTrue("Exception when fetching string " + ee.toString(), false);
      }
    }
  }

  @Test
  public void getString_onDemandLoadingMultipleStrings_calledFromMultipleThreads() throws InterruptedException {
    int numberOfThreads = 100;
    int numStringToCall = EXPECTED_STRINGS.length;
    ExecutorService service = Executors.newFixedThreadPool(numberOfThreads);
    CountDownLatch latch = new CountDownLatch(numberOfThreads);

    List<Callable<List<String>>> tasks = new ArrayList<>();
    Random random = new Random();
    for (int i = 0; i < numberOfThreads; i++) {
      final int nextRandomNumber = random.nextInt(numStringToCall - 1) + 1;
      // Add tasks to be called
      tasks.add(() -> {
        List<String> outcome = new ArrayList<>();
        outcome.add(EXPECTED_STRINGS[nextRandomNumber]);
        outcome.add(parsedStringPack.getString(nextRandomNumber));
        latch.countDown();
        return outcome;
      });
    }
    List<Future<List<String>>> results = service.invokeAll(tasks, 5, TimeUnit.SECONDS);
    latch.await();
    // To allow any remaining tasks to return their result since latch.countDown happened before
    // returning the string
    Thread.sleep(2000);
    for (int i = 0; i < numberOfThreads; i++) {
      Future<List<String>> result = results.get(i);
      assertTrue(!result.isCancelled());
      try {
        List<String> output = result.get();
        assertEquals("Problem fetching correct string", output.get(0), output.get(1));
      } catch (ExecutionException ee) {
        assertTrue("Exception when fetching string " + ee.toString(), false);
      }
    }
  }

  @Test
  public void getQuantityString_onDemandLoading_calledFromMultipleThreads() throws InterruptedException {
    int numberOfThreads = 100;
    String[] expected = {"零个", "一个", "两个", "少许", "多数", "其他"};
    int numQuantityStrings = expected.length;
    ExecutorService service = Executors.newFixedThreadPool(numberOfThreads);
    CountDownLatch latch = new CountDownLatch(numberOfThreads);

    Callable<String[]> getQuantityStringTask = () -> {
      String[] result = new String[numQuantityStrings];
      for (int i = 0; i < numQuantityStrings; i++) {
        result[i] = parsedStringPack.getQuantityString(PLURALS_ID, (long) i, TEST_PLURAL_RULES);
      }
      latch.countDown();
      return result;
    };
    List<Callable<String[]>> tasks = new ArrayList<>();
    for (int i = 0; i < numberOfThreads; i++) {
      tasks.add(getQuantityStringTask);
    }
    List<Future<String[]>> results = service.invokeAll(tasks, 2, TimeUnit.SECONDS);
    latch.await();
    // To allow any remaining tasks to return their result since latch.countDown happened before
    // returning the string
    Thread.sleep(2000);
    for (int i = 0; i < numberOfThreads; i++) {
      Future<String[]> result = results.get(i);
      assertTrue(!result.isCancelled());
      try {
        String[] actual = result.get();
        for (int j = 0; j < numQuantityStrings; j++) {
          assertEquals("Correct plural not fetched", expected[j], actual[j]);
        }
      } catch (ExecutionException ee) {
        assertTrue("Exception when fetching plurals " + ee.toString(), false);
      }
    }
  }
}
