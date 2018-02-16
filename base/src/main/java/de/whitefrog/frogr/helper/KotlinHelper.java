package de.whitefrog.frogr.helper;

public class KotlinHelper {
  public static Enum getEnumValue(Class<Enum> clazz, String name) {
    return Enum.valueOf(clazz, name);
  }
}
