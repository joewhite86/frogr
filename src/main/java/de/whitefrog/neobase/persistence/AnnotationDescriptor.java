package de.whitefrog.neobase.persistence;

import de.whitefrog.neobase.model.annotation.Indexed;
import de.whitefrog.neobase.model.annotation.RelatedTo;
import de.whitefrog.neobase.model.annotation.RelationshipCount;

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
