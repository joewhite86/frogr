package de.whitefrog.frogr.helper;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * <p>Utility class that uses {@code java.lang.reflect} standard library.
 * It provides easy access to the standard reflect methods that are
 * needed usually when dealing with generic object types.</p>
 * 
 * <p>- Stripped down to only needed methods -</p>
 *
 * @author Qussay Najjar
 * @version 1.1
 * @link http://qussay.com/2013/09/28/handling-java-generic-types-with-reflection
 * @since 2014-04-13
 */
public class ReflectionUtil {

  /**
   * When {@code Type} initialized with a value of an object, its fully qualified class name
   * will be prefixed with this.
   *
   * @see ReflectionUtil#getClassName(Type)
   */
  private static final String TYPE_CLASS_NAME_PREFIX = "class ";
  private static final String TYPE_INTERFACE_NAME_PREFIX = "interface ";

  /*
   *  Utility class with static access methods, no need for constructor.
   */
  private ReflectionUtil() {
  }

  /**
   * {@link Type#toString()} value is the fully qualified class name prefixed
   * with {@link ReflectionUtil#TYPE_CLASS_NAME_PREFIX}. This method will substring it, for it to be eligible
   * for {@link Class#forName(String)}.
   *
   * @param type the {@code Type} value whose class name is needed.
   * @return {@code String} class name of the invoked {@code type}.
   * @see ReflectionUtil#getClass()
   */
  public static String getClassName(Type type) {
    if(type == null) {
      return "";
    }
    String className = type.toString();
    if(className.startsWith(TYPE_CLASS_NAME_PREFIX)) {
      className = className.substring(TYPE_CLASS_NAME_PREFIX.length());
    } else if(className.startsWith(TYPE_INTERFACE_NAME_PREFIX)) {
      className = className.substring(TYPE_INTERFACE_NAME_PREFIX.length());
    }
    return className;
  }

  /**
   * Returns the {@code Class} object associated with the given {@link Type}
   * depending on its fully qualified name.
   *
   * @param type the {@code Type} whose {@code Class} is needed.
   * @return the {@code Class} object for the class with the specified name.
   * @throws ClassNotFoundException if the class cannot be located.
   * @see ReflectionUtil#getClassName(Type)
   */
  public static Class<?> getClass(Type type)
    throws ClassNotFoundException {
    String className = getClassName(type);
    if(className == null || className.isEmpty()) {
      return null;
    }
    return Class.forName(className);
  }
  
  public static Class<?> getGenericClass(Field field) {
    ParameterizedType type = (ParameterizedType) field.getGenericType();
    return (Class<?>) type.getActualTypeArguments()[0];
  }

  /**
   * Returns an array of {@code Type} objects representing the actual type
   * arguments to this object.
   * If the returned value is null, then this object represents a non-parameterized
   * object.
   *
   * @param object the {@code object} whose type arguments are needed.
   * @return an array of {@code Type} objects representing the actual type
   * arguments to this object.
   * @see Class#getGenericSuperclass()
   * @see ParameterizedType#getActualTypeArguments()
   */
  public static Type[] getParameterizedTypes(Object object) {
    Type superclassType = object.getClass().getGenericSuperclass();
    if(!ParameterizedType.class.isAssignableFrom(superclassType.getClass())) {
      return null;
    }

    return ((ParameterizedType) superclassType).getActualTypeArguments();
  }

  public static Field getSuperField(Class clazz, String name) throws NoSuchFieldException {
    Field field = null;
    while(clazz != null) {
      try {
        field = clazz.getDeclaredField(name);
        break;
      } catch(NoSuchFieldException e) {
        clazz = clazz.getSuperclass();
      }
    }
    if(field == null) {
      throw new NoSuchFieldException(name);
    }
    return field;
  }
}