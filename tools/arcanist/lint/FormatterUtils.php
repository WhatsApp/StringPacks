<?php
// Copyright 2017-present Facebook. All Rights Reserved.

/**
 * These are utilities to turn the differences between two texts into
 * individual lint messages.
 */
final class FormatterUtils extends Phobject {
  /**
   * Runs FormatterUtils::buildChanges, and returns the results in ready-made
   * ArcanistLintMessage's.
   */
  public static function buildLintMessages(
    $path, $code, $severity, $name, $desc, $old, $new) {
    $messages = array();
    if ($old !== $new) {
      $changes = self::buildChanges($old, $new);
      foreach ($changes as $change) {
        $messages[] = id(new ArcanistLintMessage())
          ->setPath($path)
          ->setLine($change['line'])
          ->setChar($change['char'])
          ->setCode($code)
          ->setSeverity($severity)
          ->setName($name)
          ->setDescription($desc)
          ->setOriginalText($change['original'])
          ->setReplacementText($change['replacement']);
      }
    }
    return $messages;
  }

  /**
   * Computes "changes" between two texts and returns:
   *   [
   *     {
   *       line: number,
   *       char: number,
   *       original: string,
   *       replacement: string,
   *     },
   *   ]
   */
  public static function buildChanges($old, $new) {
    $file_old = new TempFile();
    $file_new = new TempFile();

    Filesystem::writeFile($file_old, (string)$old);
    Filesystem::writeFile($file_new, (string)$new);

    list($err, $stdout) = exec_manual('diff -U0 %s %s', $file_old, $file_new);

    $patches = array();
    $chunks = preg_split('/^(?=@@)/m', $stdout);
    $lead_chunk = array_shift($chunks);

    if (!preg_match('/^---.*\n\+\+\+.*\n$/', $lead_chunk)) {
      throw new Exception('Unexpected diff output start');
    }

    foreach ($chunks as $chunk) {
      $patch = array(
        'line' => null,
        'char' => 1,
        'original' => '',
        'replacement' => '',
      );

      $lines = phutil_split_lines($chunk, /* retain_endings */ true);
      foreach ($lines as $i => $line) {
        if ($line[0] === '@') {
          $matches = null;
          preg_match('/^@@ -(\d+)/', $line, $matches);
          if (!$matches) {
            throw new Exception('Unexpected diff header line: '.$line);
          }
          $patch['line'] = (int)$matches[1];
        } else if ($line[0] === '-') {
          $patch['original'] .= substr($line, 1);
        } else if ($line[0] === '+') {
          $patch['replacement'] .= substr($line, 1);
        } else if ($line[0] === '\\') {
          // Handle "\ No newline at end of file"
          $prev_line = $lines[$i - 1];
          if ($prev_line[0] === '-') {
            $patch['original'] = preg_replace('/\r?\n$/', '', $patch['original']);
          } else if ($prev_line[0] === '+') {
            $patch['replacement'] = preg_replace('/\r?\n$/', '', $patch['replacement']);
          } else {
            throw new Exception('Unexpected diff prev hunk: '.$line);
          }
        } else {
          throw new Exception('Unexpected diff hunk line: '.$line);
        }
      }

      // No "original" means we're inserting a line. Arcanist expects the
      // insertion point to be the next line.
      if (!$patch['original']) {
        $patch['line']++;
      }

      $patches[] = $patch;
    }

    return $patches;
  }

  /**
   * Takes the values from `$this->getEngine()->getPathChangedLines(...)` and
   * returns ranges like `['1:3', '5:5', '7:10'].
   */
  public static function changedRanges($changed_lines) {
    $ranges = array();
    if ($changed_lines) {
      $numbers = array_keys($changed_lines);
      $lastindex = count($numbers) - 1;
      $start = $numbers[0];
      foreach ($numbers as $i => $n) {
        if ($i === $lastindex || $numbers[$i + 1] != $n + 1) {
          $ranges[] = $start.':'.$n;
          if ($i !== $lastindex) {
            $start = $numbers[$i + 1];
          }
        }
      }
    }
    return $ranges;
  }
}
