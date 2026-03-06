package com.blockreplace.app;

import com.blockreplace.cli.BlockReplaceCli;
import com.blockreplace.gui.BlockReplaceGuiApp;

public final class Main {
  public static void main(String[] args) {
    if (args != null && args.length > 0) {
      System.exit(BlockReplaceCli.run(args));
      return;
    }
    BlockReplaceGuiApp.launchGui(args);
  }
}

