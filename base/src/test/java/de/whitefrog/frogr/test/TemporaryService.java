/*
 * Copyright (c) 2014.
 */

package de.whitefrog.frogr.test;

import de.whitefrog.frogr.Service;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.test.TestGraphDatabaseFactory;

public class TemporaryService extends Service {
  @Override
  public void connect() {
    register("de.whitefrog.frogr");
    super.connect();
  }

  @Override
  protected GraphDatabaseService createGraphDatabase() {
    return new TestGraphDatabaseFactory()
      .newImpermanentDatabaseBuilder()
      .newGraphDatabase();
  }
}
