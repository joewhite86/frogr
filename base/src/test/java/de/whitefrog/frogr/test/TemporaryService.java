/*
 * Copyright (c) 2014.
 */

package de.whitefrog.frogr.test;

import de.whitefrog.frogr.Service;
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
        register("de.whitefrog.frogr");
        path = folder.newFolder().getAbsolutePath();
      }
      super.connect(path);
      System.out.println("GraphDb @ " + path);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  
  public void softShutdown() {
    super.shutdown();
  }

  @Override
  public void shutdown() {
    super.shutdown();
    folder.delete();
  }
}
