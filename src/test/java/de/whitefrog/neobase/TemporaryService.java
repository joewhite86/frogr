/*
 * Copyright (c) 2014.
 */

package de.whitefrog.neobase;

import org.junit.rules.TemporaryFolder;

import java.io.IOException;

public class TemporaryService extends Service {
  private final TemporaryFolder folder = new TemporaryFolder();

  @Override
  public void connect() {
    try {
      folder.create();
      super.connect(folder.newFolder().getAbsolutePath());
      System.out.println("GraphDb @ " + folder.newFolder().getAbsolutePath());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void shutdown() {
    super.shutdown();
    folder.delete();
  }
}
