package de.whitefrog.neobase.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import de.whitefrog.neobase.exception.NeobaseRuntimeException;
import de.whitefrog.neobase.model.annotation.Indexed;
import de.whitefrog.neobase.model.annotation.NotPersistant;
import de.whitefrog.neobase.model.annotation.Unique;
import de.whitefrog.neobase.model.annotation.Uuid;
import de.whitefrog.neobase.persistence.FieldDescriptor;
import de.whitefrog.neobase.persistence.Persistence;
import de.whitefrog.neobase.rest.Views;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public abstract class Entity implements Model, Comparable<Base> {
  @JsonView({Views.Hidden.class})
  private String model;
  @Uuid
  @Unique
  @Indexed
  private String uuid;
  private static final Random random = new Random();

  private String type;
  @JsonView({Views.Hidden.class})
  private long id = random.nextLong();
  @JsonView({Views.Secure.class})
  private Long created;
  @JsonView({Views.Secure.class})
  private Long lastModified;
  @JsonView({Views.Hidden.class})
  private String modifiedBy;

  @NotPersistant
  private boolean initialId = true;
  @NotPersistant
  private List<String> checkedFields = new ArrayList<>();
  @NotPersistant
  private List<String> fetchedFields = new ArrayList<>();

  public long getId() {
    return initialId? -1: id;
  }

  public void setId(long id) {
    this.id = id;
    this.initialId = false;
  }

  @Override
  public <T extends Base> T clone(String... fields) {
    return clone(Arrays.asList(fields));
  }
  
  @Override
  public <T extends Base> T clone(List<String> fields) {
    T base;
    try {
      base = (T) getClass().newInstance();
      base.setType(type());
      base.setId(getId());
      base.setUuid(getUuid());
      for(FieldDescriptor descriptor: Persistence.cache().fieldMap(getClass())) {
        if(fields.contains(descriptor.getName()) || 
            (descriptor.annotations().relatedTo == null && fields.contains(Base.AllFields))) {
          descriptor.field().setAccessible(true);
          descriptor.field().set(base, descriptor.field().get(this));
        }
      }
    } catch(ReflectiveOperationException e) {
      throw new NeobaseRuntimeException(e.getMessage(), e);
    }
    return base;
  }

  @Override
  public String type() {
    return type;
  }

  @Override
  public String getType() {
    return type;
  }

  @Override
  public void setType(String type) {
    this.type = type;
  }

  @Override
  public void addCheckedField(String field) {
    checkedFields.add(field);
  }

  @Override
  @JsonIgnore
  public List<String> getCheckedFields() {
    return checkedFields;
  }

  @Override
  public void setCheckedFields(List<String> checkedFields) {
    this.checkedFields = checkedFields;
  }

  @Override
  public Long getCreated() {
    return created;
  }

  @Override
  public void setCreated(long created) {
    this.created = created;
  }

  @Override
  @JsonIgnore
  public List<String> getFetchedFields() {
    return fetchedFields;
  }

  @Override
  public void setFetchedFields(List<String> fetchedFields) {
    this.fetchedFields = fetchedFields;
  }

  @Override
  public Long getLastModified() {
    return lastModified;
  }

  @Override
  public void setLastModified(long lastModified) {
    this.lastModified = lastModified;
  }

  @Override
  public void updateLastModified() {
    this.lastModified = System.currentTimeMillis();
  }

  @Override
  public String getModifiedBy() {
    return modifiedBy;
  }

  @Override
  public void setModifiedBy(String modifiedBy) {
    this.modifiedBy = modifiedBy;
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37)
      .append(getId())
      .toHashCode();
  }

  @Override
  public String toString() {
    return (type() != null? type(): getClass().getSimpleName()) + " (" + getId() + ")";
  }

  @Override
  public void resetId() {
    id = random.nextLong();
    initialId = true;
  }

  @Override
  public String getUuid() {
    return uuid;
  }

  @Override
  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  @Override
  public String getModel() {
    return model;
  }

  @Override
  public void setModel(String model) {
    this.model = model;
  }
  
  @Override
  public boolean isPersisted() {
    return getId() > 0 || getUuid() != null;
  }

  @Override
  public int compareTo(Base o) {
    return Long.compare(getId(), o.getId());
  }

  @Override
  public boolean equals(Object o) {
    if(this == o) return true;
    if(!(o instanceof Entity)) return false;

    Entity base = (Entity) o;

    if(getUuid() != null && base.getUuid() != null) {
      if(!getUuid().equals(base.getUuid())) return false;
    }
    else if(getId() != base.getId()) return false;

    if(!getClass().equals(base.getClass())) return false;

    return true;
  }
}
