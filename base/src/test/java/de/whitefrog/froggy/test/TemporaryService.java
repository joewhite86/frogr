/*
 * Copyright (c) 2014.
 */

package de.whitefrog.froggy.test;

import de.whitefrog.froggy.Service;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;

public class TemporaryService extends Service {
  private final TemporaryFolder folder = new TemporaryFolder();
  private String path;

  @Override
  public void connect() {
    try {
      if(path == null) {
        folder.create();
        register("de.whitefrog.froggy");
        path = folder.newFolder().getAbsolutePath();
      }
      super.connect(path);
      System.out.println("GraphDb @ " + path);
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
