package com.blockreplace.cli;

import picocli.CommandLine.Option;

public final class SharedOptions {
  @Option(
      names = {"--ansi"},
      description = "Enable ANSI colors in console output (default: false).")
  boolean ansi = false;
}

