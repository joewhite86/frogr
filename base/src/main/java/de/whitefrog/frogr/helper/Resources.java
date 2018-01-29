package de.whitefrog.frogr.helper;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public abstract class Resources {
  public static InputStream stream(String path) throws IOException, URISyntaxException {
    return Files.newInputStream(path(path));
  }
  public static BufferedReader reader(String path) throws IOException, URISyntaxException {
    return Files.newBufferedReader(path(path));
  }
  public static Path path(String path) throws IOException, URISyntaxException {
    Path filepath = File.createTempFile("res-cpy", "xyz").toPath();
    Files.copy(ClassLoader.getSystemResourceAsStream(path), filepath, StandardCopyOption.REPLACE_EXISTING);
    return filepath;
  }
}
