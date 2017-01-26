package de.whitefrog.neobase.model.rest;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import java.util.Arrays;

/**
* Created by jochen on 12/01/16.
*/
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY,
  getterVisibility = JsonAutoDetect.Visibility.NONE,
  setterVisibility = JsonAutoDetect.Visibility.NONE)
public class QueryField {
  private String field;
  @JsonInclude(JsonInclude.Include.NON_DEFAULT)
  private int skip = 0;
  @JsonInclude(JsonInclude.Include.NON_DEFAULT)
  private int limit = SearchParameter.DefaultLimit;
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private FieldList subFields = new FieldList();

  public QueryField() {}
  public QueryField(String field) {
    this(field, false);
  }
  public QueryField(String field, boolean addAll) {
    if(field.contains(".")) {
      String[] fields = field.split("\\.", 2);
      parseField(fields[0]);
      this.subFields = FieldList.parseFields(Arrays.asList(fields[1]), addAll);
    } else {
      parseField(field);
    }
  }
  
  private void parseField(String field) {
    if(field.contains("(")) {
      this.field = field.substring(0, field.indexOf("("));
      String limit = field.substring(field.indexOf("(") + 1, field.length() - 1);
      if(limit.contains(";")) {
        this.skip = Integer.parseInt(limit.substring(0, limit.indexOf(";")));
        limit = limit.substring(limit.indexOf(";") + 1);
        this.limit = limit.equals("max")? Integer.MAX_VALUE: Integer.parseInt(limit);
      } else {
        this.limit = limit.equals("max")? Integer.MAX_VALUE: Integer.parseInt(limit);
      }
    } else {
      this.field = field;
    }
  }

  public String field() {
    return field;
  }

  public int limit() {
    return limit;
  }

  public void limit(int limit) {
    this.limit = limit;
  }
  
  public int skip() { return skip; }
  public void skip(int skip) { this.skip = skip; }

  public FieldList subFields() {
    return subFields;
  }

  public void subFields(FieldList fields) {
    this.subFields = fields;
  }
  
  public void subFields(QueryField... fd) {
    this.subFields.addAll(Arrays.asList(fd));
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder()
      .append(field)
      .toHashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if(!(obj instanceof QueryField)) return false;
    QueryField other = (QueryField) obj;
    return new EqualsBuilder()
      .append(field, other.field)
      .isEquals();
  }

  @Override
  public String toString() {
    return field + (subFields().isEmpty()? "": " (" + subFields.size() + " subfields)");
  }
}
