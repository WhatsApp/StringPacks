<?php
// This software contains information and intellectual property that is
// confidential and proprietary to Facebook, Inc. and its affiliates.

final class WhatsAppAndroidKotlinFormatLinter extends ArcanistFutureLinter {
  const LINT_FORMAT = 0;
  const LINT_KOTLIN_ERROR = 1;
  const LINT_UNKNOWN_ERROR = 2;

  public function getLinterName() {
    return 'KTFMT';
  }

  public function getLintNameMap() {
    return array(
      self::LINT_FORMAT => 'Format Mismatch',
      self::LINT_KOTLIN_ERROR => 'Formatter Error',
      self::LINT_UNKNOWN_ERROR => 'Unknown Error',
    );
  }

  public function getLintSeverityMap() {
    return array(
      self::LINT_FORMAT => ArcanistLintSeverity::SEVERITY_AUTOFIX,
      self::LINT_KOTLIN_ERROR => ArcanistLintSeverity::SEVERITY_ERROR,
      self::LINT_UNKNOWN_ERROR => ArcanistLintSeverity::SEVERITY_ADVICE,
    );
  }

  protected function buildFutures(array $paths) {
    $futures = array();
    foreach ($paths as $path) {
      $futures[$path] = id(new ExecFuture("./tools/third-party/ktfmt/ktfmt -"))
             ->write($this->getData($path))
             ->setCWD($this->getProjectRoot());
    }
    return $futures;
  }

  protected function resolveFuture($path, Future $future) {
    list($err, $stdout, $stderr) = $future->resolve();
    if ($err) {
      $messages = self::parseErrorOutput($path, $future);
    } else {
      $messages = FormatterUtils::buildLintMessages(
        $path,
        $this->getLintMessageFullCode(self::LINT_FORMAT),
        $this->getLintMessageSeverity(self::LINT_FORMAT),
        $this->getLintMessageName(self::LINT_FORMAT),
        'The changes in this file are formatted inconsistently with our code style.'
          .' Applying the suggested fix from lint will fix this for you.'
          .' https://fburl.com/fbsource-linters#ktfmt',
        $this->getData($path),
        $stdout);
    }
    foreach ($messages as $message) {
      $message->setBypassChangedLineFiltering(true);
      $this->addLintMessage($message);
    }
  }

  private function parseErrorOutput($path, Future $future) {
    list($err, $stdout, $stderr) = $future->resolve();
    $messages = array();
    $lines = phutil_split_lines($stderr, false);
    foreach ($lines as $line) {
      $matches = null;
      preg_match(
        '/^(?P<file>[^:]+):(?:(?P<line>\d+):)?(?:(?P<column>\d+):)?(?:\s*)?error: (?P<message>.*)$/',
        $line,
        $matches);
      if ($matches) {
        $messages[] = id(new ArcanistLintMessage())
          ->setPath($path)
          ->setLine((int)$matches['line'])
          ->setChar((int)$matches['column'] - 1)
          ->setCode($this->getLintMessageFullCode(self::LINT_KOTLIN_ERROR))
          ->setName($this->getLintMessageName(self::LINT_KOTLIN_ERROR))
          ->setDescription($matches['message'])
          ->setSeverity($this->getLintMessageSeverity(self::LINT_KOTLIN_ERROR));
      }
    }
    if (!$messages) {
      $exception = new CommandException(
        sprintf('ktfmt exit code %s.', $err),
        $future->getCommand(),
        $err,
        $stdout,
        $stderr);
      $messages[] = id(new ArcanistLintMessage())
        ->setPath($path)
        ->setCode($this->getLintMessageFullCode(self::LINT_UNKNOWN_ERROR))
        ->setName($this->getLintMessageName(self::LINT_UNKNOWN_ERROR))
        ->setDescription($exception->getMessage())
        ->setSeverity($this->getLintMessageSeverity(self::LINT_UNKNOWN_ERROR));
    }
    return $messages;
  }
}
