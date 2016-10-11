package de.whitefrog.neobase.model.rest;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import de.whitefrog.neobase.model.Base;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
* Created by jochen on 12/01/16.
*/
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY,
  getterVisibility = JsonAutoDetect.Visibility.NONE,
  setterVisibility = JsonAutoDetect.Visibility.NONE)
public class FieldList extends HashSet<QueryField> {
  public FieldList() {
    super();
  }

  public static FieldList create(QueryField... fields) {
    FieldList list = new FieldList();
    list.addAll(Arrays.asList(fields));
    return list;
  }
  public static FieldList parseFields(String... fields) {
    return parseFields(Arrays.asList(fields), false);
  }
  public static FieldList parseFields(List<String> fields) {
    return parseFields(fields, false);
  }
  public static FieldList parseFields(List<String> fields, boolean addAll) {
    FieldList fieldList = new FieldList();
    
    for(String field: fields) {
      if(field.contains(".")) {
        String fieldName = field.substring(0, field.indexOf("."));
        if(fieldList.containsField(fieldName)) {
          fieldList.get(fieldName).subFields(new QueryField(field.substring(field.indexOf(".") + 1), addAll));
          continue;
        }
      } else if(field.startsWith("[")) {
        // assuming sth like user.[name;login]
        return parseFields(field.substring(1, field.length() - 1).split(";"));
      }
      QueryField queryField = new QueryField(field, addAll);
      if(addAll) queryField.subFields(new QueryField(Base.AllFields));
      fieldList.add(queryField);
    }

    if(addAll) fieldList.add(new QueryField(Base.AllFields));
    return fieldList;
  }

  public boolean containsField(String name) {
    for(QueryField descriptor: this) {
      if(descriptor.field().equals(name)) return true;
    }
    return false;
  }

  public QueryField get(String name) {
    for(QueryField descriptor: this) {
      if(descriptor.field().equals(name)) return descriptor;
    }
    return null;
  }

}
