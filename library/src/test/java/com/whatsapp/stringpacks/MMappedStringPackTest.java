/* Copyright (c) Facebook, Inc. and its affiliates. All rights reserved.
 *
 * This source code is licensed under the Apache 2.0 license found in
 * the LICENSE file in the root directory of this source tree.
 */

package com.whatsapp.stringpacks;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
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
public class MMappedStringPackTest {

  private ParsedStringPack parsedStringPack;

  @Before
  public void setUp() {
    try {
      InputStream inputStream = getClass().getClassLoader().getResourceAsStream("strings_zh.pack");

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      byte[] buffer = new byte[1024];
      int len;
      while ((len = inputStream.read(buffer)) > -1) {
        baos.write(buffer, 0, len);
      }
      baos.flush();

      byte[] bytes = baos.toByteArray();
      RandomAccessFile randomAccessFile =
          new RandomAccessFile(
              getClass().getClassLoader().getResource("strings_zh.pack").getPath(), "r");
      FileChannel fileChannel = randomAccessFile.getChannel();
      MappedByteBuffer mappedByteBuffer =
          fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, bytes.length);

      parsedStringPack = new ParsedStringPack(Collections.singletonList("zh"), mappedByteBuffer);
    } catch (IOException e) {
      assertWithMessage("Test setup failure" + e).fail();
    }
  }

  @Test
  public void getString() {
    String stringMMapped = parsedStringPack.getString(StringPacksTestData.STRING_ID);
    assertThat(stringMMapped).isEqualTo("你好，世界");
  }

  @Test
  public void getString_MultipleTimes() {
    String firstMMapped = parsedStringPack.getString(StringPacksTestData.STRING_ID);
    assertThat(firstMMapped).isEqualTo("你好，世界");

    String secondMMapped = parsedStringPack.getString(StringPacksTestData.STRING_ID);
    assertThat(secondMMapped).isEqualTo("你好，世界");
  }

  @Test
  public void getString_WithNonexistentId() {
    String nonexistentMMapped =
        parsedStringPack.getString(StringPacksTestData.EXPECTED_STRINGS.length + 1);
    assertThat(nonexistentMMapped).isNull();
  }

  @Test
  public void getPlural_WithNonexistentId() {
    String nonexistentMMapped =
        parsedStringPack.getQuantityString(
            StringPacksTestData.PLURALS_ID + 1, 2, StringPacksTestData.TEST_PLURAL_RULES);
    assertThat(nonexistentMMapped).isNull();
  }

  @Test
  public void getPlurals_WithTestPluralRules() {
    String[] expectedQuantityStrings = {"零个", "一个", "两个", "少许", "多数", "其他"};
    for (int i = 0; i < expectedQuantityStrings.length; i++) {
      String string =
          parsedStringPack.getQuantityString(
              StringPacksTestData.PLURALS_ID, (long) i, StringPacksTestData.TEST_PLURAL_RULES);
      assertThat(string).isEqualTo(expectedQuantityStrings[i]);
    }
  }

  @Test
  public void getString_onDemandLoadingSameString_calledFromMultipleThreads()
      throws InterruptedException {
    int numberOfThreads = 100;
    String expected = "你好，世界";
    ExecutorService service = Executors.newFixedThreadPool(numberOfThreads);
    CountDownLatch latch = new CountDownLatch(numberOfThreads);

    Callable<String> getStringTaskMMap =
        () -> {
          String string = parsedStringPack.getString(StringPacksTestData.STRING_ID);
          latch.countDown();
          return string;
        };
    List<Callable<String>> tasksWithMMap = new ArrayList<>();
    for (int i = 0; i < numberOfThreads; i++) {
      tasksWithMMap.add(getStringTaskMMap);
    }
    List<Future<String>> resultsWithMMap = service.invokeAll(tasksWithMMap, 2, TimeUnit.SECONDS);
    latch.await();
    // To allow any remaining tasks to return their result since latch.countDown happened before
    // returning the string
    Thread.sleep(2000);
    for (int i = 0; i < numberOfThreads; i++) {
      Future<String> result = resultsWithMMap.get(i);
      assertThat(!result.isCancelled()).isTrue();
      try {
        assertWithMessage("Problem fetching correct string")
            .that(result.get())
            .isEqualTo(expected);
      } catch (ExecutionException ee) {
        assertWithMessage("Exception when fetching string " + ee.toString()).that(false).isTrue();
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
    Random random = new Random();

    List<Callable<List<String>>> tasksWithMMap = new ArrayList<>();
    for (int i = 0; i < numberOfThreads; i++) {
      final int nextRandomNumber = random.nextInt(numStringToCall - 1) + 1;
      // Add tasks to be called
      tasksWithMMap.add(
          () -> {
            List<String> outcome = new ArrayList<>();
            outcome.add(StringPacksTestData.EXPECTED_STRINGS[nextRandomNumber]);
            outcome.add(parsedStringPack.getString(nextRandomNumber));
            latch.countDown();
            return outcome;
          });
    }
    List<Future<List<String>>> resultsWithMMap =
        service.invokeAll(tasksWithMMap, 5, TimeUnit.SECONDS);
    latch.await();
    // To allow any remaining tasks to return their result since latch.countDown happened before
    // returning the string
    Thread.sleep(2000);
    for (int i = 0; i < numberOfThreads; i++) {
      Future<List<String>> result = resultsWithMMap.get(i);
      assertThat(!result.isCancelled()).isTrue();
      try {
        List<String> output = result.get();
        assertWithMessage("Problem fetching correct string")
            .that(output.get(1))
            .isEqualTo(output.get(0));
      } catch (ExecutionException ee) {
        assertWithMessage("Exception when fetching string " + ee.toString()).that(false).isTrue();
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

    Callable<String[]> getQuantityStringTaskMMap =
        () -> {
          String[] result = new String[numQuantityStrings];
          for (int i = 0; i < numQuantityStrings; i++) {
            result[i] =
                parsedStringPack.getQuantityString(
                    StringPacksTestData.PLURALS_ID,
                    (long) i,
                    StringPacksTestData.TEST_PLURAL_RULES);
          }
          latch.countDown();
          return result;
        };
    List<Callable<String[]>> tasksWithMMap = new ArrayList<>();
    for (int i = 0; i < numberOfThreads; i++) {
      tasksWithMMap.add(getQuantityStringTaskMMap);
    }
    List<Future<String[]>> resultsWithMMap = service.invokeAll(tasksWithMMap, 2, TimeUnit.SECONDS);
    latch.await();
    // To allow any remaining tasks to return their result since latch.countDown happened before
    // returning the string
    Thread.sleep(2000);
    for (int i = 0; i < numberOfThreads; i++) {
      Future<String[]> result = resultsWithMMap.get(i);
      assertThat(!result.isCancelled()).isTrue();
      try {
        String[] actual = result.get();
        for (int j = 0; j < numQuantityStrings; j++) {
          assertWithMessage("Correct plural not fetched").that(actual[j]).isEqualTo(expected[j]);
        }
      } catch (ExecutionException ee) {
        assertWithMessage("Exception when fetching plurals " + ee.toString()).that(false).isTrue();
      }
    }
  }
}
