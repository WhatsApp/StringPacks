/* Copyright (c) Facebook, Inc. and its affiliates. All rights reserved.
 *
 * This source code is licensed under the Apache 2.0 license found in
 * the LICENSE file in the root directory of this source tree.
 */

package com.whatsapp.stringpacks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
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

@RunWith(RobolectricTestRunner.class)
public class LoadedStringPackTest {

  private ParsedStringPack parsedStringPack;

  @Before
  public void setUp() {
    InputStream inputStream = getClass().getClassLoader().getResourceAsStream("strings_zh.pack");

    parsedStringPack = new ParsedStringPack(inputStream, Collections.singletonList("zh"), null);
  }

  @Test
  public void getString() {
    String string = parsedStringPack.getString(StringPacksTestData.STRING_ID, false);
    assertEquals("你好，世界", string);
  }

  @Test
  public void getString_MultipleTimes() {
    String first = parsedStringPack.getString(StringPacksTestData.STRING_ID, false);
    assertEquals("你好，世界", first);

    String second = parsedStringPack.getString(StringPacksTestData.STRING_ID, false);
    assertEquals("你好，世界", second);
  }

  @Test
  public void getString_WithNonexistentId() {
    String nonexistent =
        parsedStringPack.getString(StringPacksTestData.EXPECTED_STRINGS.length + 1, false);
    assertNull(nonexistent);
  }

  @Test
  public void getPlurals_WithTestPluralRules() {
    String[] expectedQuantityStrings = {"零个", "一个", "两个", "少许", "多数", "其他"};
    for (int i = 0; i < expectedQuantityStrings.length; i++) {
      String string =
          parsedStringPack.getQuantityString(
              StringPacksTestData.PLURALS_ID,
              (long) i,
              StringPacksTestData.TEST_PLURAL_RULES,
              false);
      assertEquals(expectedQuantityStrings[i], string);
    }
  }

  @Test
  public void getString_onDemandLoadingSameString_calledFromMultipleThreads()
      throws InterruptedException {
    int numberOfThreads = 100;
    String expected = "你好，世界";
    ExecutorService service = Executors.newFixedThreadPool(numberOfThreads);
    CountDownLatch latch = new CountDownLatch(numberOfThreads);

    Callable<String> getStringTask =
        () -> {
          String string = parsedStringPack.getString(StringPacksTestData.STRING_ID, false);
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
  public void getString_onDemandLoadingMultipleStrings_calledFromMultipleThreads()
      throws InterruptedException {
    int numberOfThreads = 100;
    int numStringToCall = StringPacksTestData.EXPECTED_STRINGS.length;
    ExecutorService service = Executors.newFixedThreadPool(numberOfThreads);
    CountDownLatch latch = new CountDownLatch(numberOfThreads);

    List<Callable<List<String>>> tasks = new ArrayList<>();
    Random random = new Random();
    for (int i = 0; i < numberOfThreads; i++) {
      final int nextRandomNumber = random.nextInt(numStringToCall - 1) + 1;
      // Add tasks to be called
      tasks.add(
          () -> {
            List<String> outcome = new ArrayList<>();
            outcome.add(StringPacksTestData.EXPECTED_STRINGS[nextRandomNumber]);
            outcome.add(parsedStringPack.getString(nextRandomNumber, false));
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
  public void getQuantityString_onDemandLoading_calledFromMultipleThreads()
      throws InterruptedException {
    int numberOfThreads = 100;
    String[] expected = {"零个", "一个", "两个", "少许", "多数", "其他"};
    int numQuantityStrings = expected.length;
    ExecutorService service = Executors.newFixedThreadPool(numberOfThreads);
    CountDownLatch latch = new CountDownLatch(numberOfThreads);

    Callable<String[]> getQuantityStringTask =
        () -> {
          String[] result = new String[numQuantityStrings];
          for (int i = 0; i < numQuantityStrings; i++) {
            result[i] =
                parsedStringPack.getQuantityString(
                    StringPacksTestData.PLURALS_ID,
                    (long) i,
                    StringPacksTestData.TEST_PLURAL_RULES,
                    false);
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
