package de.whitefrog.neobase.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.List;


@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
public interface Base extends Serializable {
  String AllFields = "allFields";
  String IdProperty = "id";
  String ModifiedBy = "modifiedBy";
  String LastModified = "lastModified";
  String Created = "created";
  String Type = "type";
  String Uuid = "uuid";

  <T extends Base> T clone(String... fields);

  <T extends Base> T clone(List<String> fields);

  String type();
  String getType();
  void setType(String type);

  String getUuid();
  void setUuid(String uuid);

  void addCheckedField(String field);
  @JsonIgnore
  List<String> getCheckedFields();
  void setCheckedFields(List<String> checkedFields);

  Long getCreated();
  void setCreated(long created);

  @JsonIgnore
  List<String> getFetchedFields();
  void setFetchedFields(List<String> fetchedFields);

  Long getLastModified();
  void setLastModified(long lastModified);
  void updateLastModified();

  String getModifiedBy();
  void setModifiedBy(String modifiedBy);

  void resetId();
  long getId();
  void setId(long id);

  @JsonIgnore
  boolean isPersisted();
}
