package de.whitefrog.neobase.model.rest;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import de.whitefrog.neobase.model.Base;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
* Created by jochen on 12/01/16.
*/
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY,
  getterVisibility = JsonAutoDetect.Visibility.NONE,
  setterVisibility = JsonAutoDetect.Visibility.NONE)
public class FieldList extends ArrayList<QueryField> {
  public FieldList() {
    super();
    add(new QueryField(Base.AllFields));
  }

  public static FieldList parseFields(String... fields) {
    return parseFields(Arrays.asList(fields));
  }
  public static FieldList parseFields(List<String> fields) {
    FieldList fieldList = new FieldList();
    
    for(String field: fields) {
      if(field.contains(".")) {
        String fieldName = field.substring(0, field.indexOf("."));
        if(fieldList.containsField(fieldName)) {
          fieldList.get(fieldName).subFields(new QueryField(field.substring(field.indexOf(".") + 1)));
          continue;
        }
      } else if(field.startsWith("[")) {
        // assuming sth like user.[name;login]
        return parseFields(field.substring(1, field.length() - 1).split(";"));
      }
      fieldList.add(new QueryField(field));
    }
    
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
