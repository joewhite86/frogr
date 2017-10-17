package de.whitefrog.froggy.persistence;

import de.whitefrog.froggy.model.annotation.Indexed;
import de.whitefrog.froggy.model.annotation.RelatedTo;
import de.whitefrog.froggy.model.annotation.RelationshipCount;

/**
 * Helper class to quickly cache important model annotations.
 * Helps to reduce reflection usage to a single time on bootstrap.
 */
public class AnnotationDescriptor {
  public Indexed indexed = null;
  public boolean unique = false;
  public RelatedTo relatedTo = null;
  public boolean notPersistant = false;
  public boolean fetch = false;
  public boolean required = false;
  public boolean nullRemove = false;
  public boolean blob = false;
  public boolean uuid = false;
  public boolean lazy = false;
  public RelationshipCount relationshipCount = null;
}
