package de.whitefrog.neobase.model.rest;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.util.Arrays;

/**
* Created by jochen on 12/01/16.
*/
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY,
  getterVisibility = JsonAutoDetect.Visibility.NONE,
  setterVisibility = JsonAutoDetect.Visibility.NONE)
public class QueryField {
  private String field;
  private int skip = 0;
  private int limit = SearchParameter.DefaultLimit;
  private FieldList subFields;

  public QueryField(String field) {
    if(field.contains(".")) {
      String[] fields = field.split("\\.", 2);
      parseField(fields[0]);
      this.subFields = FieldList.parseFields(fields[1]);
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
    return subFields == null? new FieldList(): subFields;
  }

  public void subFields(FieldList fields) {
    this.subFields = fields;
  }
  
  public void subFields(QueryField... fd) {
    this.subFields.addAll(Arrays.asList(fd));
  }
}
