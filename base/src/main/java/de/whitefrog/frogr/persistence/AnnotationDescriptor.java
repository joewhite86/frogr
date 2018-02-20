package de.whitefrog.frogr.persistence;

import de.whitefrog.frogr.model.annotation.*;

/**
 * Helper class to quickly cache important model annotations.
 * Helps to reduce reflection usage to a single time on bootstrap.
 */
public class AnnotationDescriptor {
  /**
   * See {@link Indexed}
   */
  public Indexed indexed = null;
  /**
   * See {@link Unique}
   */
  public boolean unique = false;
  /**
   * See {@link RelatedTo}
   */
  public RelatedTo relatedTo = null;
  /**
   * See {@link NotPersistent}
   */
  public boolean notPersistent = false;
  /**
   * See {@link Fetch}
   */
  public boolean fetch = false;
  /**
   * See {@link Required}
   */
  public boolean required = false;
  /**
   * See {@link NullRemove}
   */
  public boolean nullRemove = false;
  /**
   * See {@link Blob}
   */
  public boolean blob = false;
  /**
   * See {@link Uuid}
   */
  public boolean uuid = false;
  /**
   * See {@link Lazy}
   */
  public boolean lazy = false;
  /**
   * See {@link RelationshipCount}
   */
  public RelationshipCount relationshipCount = null;
}
