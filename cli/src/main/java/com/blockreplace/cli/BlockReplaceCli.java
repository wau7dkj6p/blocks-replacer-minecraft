package com.blockreplace.cli;

import picocli.CommandLine;

public final class BlockReplaceCli {
  private BlockReplaceCli() {}

  public static int run(String[] args) {
    return new CommandLine(new RootCommand()).execute(args);
  }
}

