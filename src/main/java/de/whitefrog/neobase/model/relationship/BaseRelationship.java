package de.whitefrog.neobase.model.relationship;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import de.whitefrog.neobase.exception.NeobaseRuntimeException;
import de.whitefrog.neobase.model.Base;
import de.whitefrog.neobase.model.annotation.Indexed;
import de.whitefrog.neobase.model.annotation.NotPersistant;
import de.whitefrog.neobase.model.annotation.Unique;
import de.whitefrog.neobase.model.annotation.Uuid;
import de.whitefrog.neobase.rest.Views;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class BaseRelationship<From extends de.whitefrog.neobase.model.Model, To extends de.whitefrog.neobase.model.Model> implements Relationship<From, To> {
  private From from;
  private To to;
  private String type;
  @Uuid
  @Unique
  @Indexed
  private String uuid;
  private static final Random random = new Random();

  @JsonView({Views.Hidden.class})
  private long id = random.nextLong();
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
  
  public BaseRelationship() {}
  public BaseRelationship(From from, To to) { this.from = from; this.to = to; }

  public long getId() {
    return initialId? -1: id;
  }

  public void setId(long id) {
    this.id = id;
    this.initialId = false;
  }

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
  public String getType() {
    return type;
  }

  @Override
  public void setType(String type) {
    this.type = type;
  }

  @Override
  public From getFrom() {
    return from;
  }

  @Override
  public Relationship setFrom(From from) {
    this.from = from;
    return this;
  }

  @Override
  public To getTo() {
    return to;
  }

  @Override
  public Relationship setTo(To to) {
    this.to = to;
    return this;
  }

  public String type() {
    return type;
  }

  public void addCheckedField(String field) {
    checkedFields.add(field);
  }

  @JsonIgnore
  public List<String> getCheckedFields() {
    return checkedFields;
  }

  public void setCheckedFields(List<String> checkedFields) {
    this.checkedFields = checkedFields;
  }

  public Long getCreated() {
    return created;
  }

  public void setCreated(long created) {
    this.created = created;
  }

  @JsonIgnore
  public List<String> getFetchedFields() {
    return fetchedFields;
  }

  public void setFetchedFields(List<String> fetchedFields) {
    this.fetchedFields = fetchedFields;
  }

  public Long getLastModified() {
    return lastModified;
  }

  public void setLastModified(long lastModified) {
    this.lastModified = lastModified;
  }

  public void updateLastModified() {
    this.lastModified = System.currentTimeMillis();
  }

  public String getModifiedBy() {
    return modifiedBy;
  }

  public void setModifiedBy(String modifiedBy) {
    this.modifiedBy = modifiedBy;
  }

  @Override
  public boolean isPersisted() {
    return getId() > 0 || getUuid() != null;
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
    } catch(ReflectiveOperationException e) {
      throw new NeobaseRuntimeException(e.getMessage(), e);
    }
    base.setType(type());
    if(fields.isEmpty() || fields.contains(IdProperty) && getId() > 0) base.setId(getId());
    return (T) base;
  }
  
  @Override
  public boolean equals(Object o) {
    if(this == o) return true;
    if(!(o instanceof Relationship)) return false;

    BaseRelationship base = (BaseRelationship) o;

    if(id != base.id) return false;

    if(!getClass().equals(base.getClass())) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(21, 41)
      .append(getId())
      .toHashCode();
  }

  @Override
  public String toString() {
    return (type() != null? type(): getClass().getSimpleName()) + " (" + getId() + ")";
  }
}
