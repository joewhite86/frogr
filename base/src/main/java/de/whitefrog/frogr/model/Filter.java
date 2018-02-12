package de.whitefrog.frogr.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.Date;
import java.util.function.Predicate;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = Filter.Equals.class, name = "eq"),
  @JsonSubTypes.Type(value = Filter.NotEquals.class, name = "neq"),
  @JsonSubTypes.Type(value = Filter.LessThan.class, name = "lt"),
  @JsonSubTypes.Type(value = Filter.GreaterThan.class, name = "gt"),
  @JsonSubTypes.Type(value = Filter.Range.class, name = "range")
})
public interface Filter extends Predicate<Object> {
  Object getValue();
  void setValue(Object value);

  String getProperty();
  void setProperty(String property);

  static Filter getStringFilter(String field, String value) {
    if(value.startsWith("*") && value.endsWith("*")) return new Filter.Contains(field, value.substring(1, value.length() - 1));
    if(value.startsWith("*")) return new Filter.EndsWith(field, value.substring(1));
    if(value.endsWith("*")) return new Filter.StartsWith(field, value.substring(0, value.length() - 1));
    return new Filter.Equals(field, value);
  }
  
  abstract class Default implements Filter {
    private Object value;
    private String property;
    
    @JsonCreator
    public Default(@JsonProperty("property") String property, @JsonProperty("value") Object value) {
      this.property = property;
      this.value = value;
    }

    @Override
    public String getProperty() {
      return property;
    }
    @Override
    public void setProperty(String property) {
      this.property = property;
    }

    @Override
    public Object getValue() {
      return value;
    }
    @Override
    public void setValue(Object value) {
      this.value = value;
    }
  }

  class Equals extends Default implements Filter, Predicate<Object> {
    @JsonCreator
    public Equals(@JsonProperty("property") String property, @JsonProperty("value") Object value) {
      super(property, value);
    }

    @Override
      public boolean test(Object other) {
          return other.equals(getValue());
      }
  }

  class NotEquals extends Default implements Filter, Predicate<Object> {
    @JsonCreator
    public NotEquals(@JsonProperty("property") String property, @JsonProperty("value") Object value) {
      super(property, value);
    }

    @Override
      public boolean test(Object other) {
          return !other.equals(getValue());
      }
  }

  class GreaterThan extends Default implements Filter, Predicate<Object> {
    private boolean including = false;

    @JsonCreator
    public GreaterThan(@JsonProperty("property") String property, @JsonProperty("value") Object value) {
      super(property, value);
    }

    public GreaterThan(String property, Object value, boolean including) {
      super(property, value);
      this.including = including;
    }

    @Override
    public Long getValue() {
      if(super.getValue() instanceof Integer)
        return ((Integer) super.getValue()).longValue();
      if(super.getValue() instanceof Date)
        return ((Date) super.getValue()).getTime();
      return (Long) super.getValue();
    }

    public boolean isIncluding() {
      return including;
    }

    public void setIncluding(boolean value) {
      including = value;
    }

    @Override
    public boolean test(Object other) {
      Long otherLong;
      if(other instanceof Integer) otherLong = new Long((Integer) other);
      else otherLong = (Long) other;
      return isIncluding()? otherLong >= getValue(): otherLong > getValue();
    }
  }

  class LessThan extends Default implements Filter, Predicate<Object> {
    private boolean including = false;

    @JsonCreator
    public LessThan(@JsonProperty("property") String property, @JsonProperty("value") Object value) {
      super(property, value);
    }
    
    public LessThan(String property, Object value, boolean including) {
      super(property, value);
      this.including = including;
    }

    public Long getValue() {
      if(super.getValue() instanceof Integer)
        return ((Integer) super.getValue()).longValue();
      if(super.getValue() instanceof Date)
        return ((Date) super.getValue()).getTime();
      return (Long) super.getValue();
    }

    public boolean isIncluding() {
      return including;
    }

    public void setIncluding(boolean value) {
      including = value;
    }

    @Override
    public boolean test(Object other) {
      Long otherLong;
      if(other instanceof Integer) otherLong = new Long((Integer) other);
      else otherLong = (Long) other;
      return isIncluding()? otherLong <= getValue(): otherLong < getValue();
    }
  }
  
  class StartsWith extends Default implements Filter, Predicate<Object> {
    @JsonCreator
    public StartsWith(@JsonProperty("property") String property, @JsonProperty("value") Object value) {
      super(property, value);
    }

    @Override
    public String getValue() {
      return (String) super.getValue();
    }

    @Override
    public boolean test(Object other) {
      return other instanceof String && ((String) other).startsWith(getValue());
    }
  }

  class EndsWith extends Default implements Filter, Predicate<Object> {
    @JsonCreator
    public EndsWith(@JsonProperty("property") String property, @JsonProperty("value") Object value) {
      super(property, value);
    }

    @Override
    public String getValue() {
      return (String) super.getValue();
    }

    @Override
    public boolean test(Object other) {
      return other instanceof String && ((String) other).endsWith(getValue());
    }
  }

  class Contains extends Default implements Filter, Predicate<Object> {
    @JsonCreator
    public Contains(@JsonProperty("property") String property, @JsonProperty("value") Object value) {
      super(property, value);
    }

    @Override
    public String getValue() {
      return (String) super.getValue();
    }

    @Override
    public boolean test(Object other) {
      return other instanceof String && ((String) other).contains(getValue());
    }
  }

  class Range extends Default implements Filter, Predicate<Object> {
    private boolean including = true;
    private long from;
    private long to;

    @JsonCreator
    public Range(@JsonProperty("property") String property, @JsonProperty("from") long from, @JsonProperty("to") long to) {
      super(property, from);
      this.from = from;
      this.to = to;
    }
    
    public Range(String property, Date from, Date to) {
      super(property, from);
      this.from = from.getTime();
      this.to = to.getTime();
    }

    public Long getValue() {
        return from;
    }

    public void setValue(Object value) {
    }

    public long getFrom() {
        return from;
    }

    public void setFrom(long from) {
        this.from = from;
    }

    public long getTo() {
        return to;
    }

    public void setTo(long to) {
        this.to = to;
    }

    public boolean isIncluding() {
        return including;
    }

    public void setIncluding(boolean value) {
        including = value;
    }

    @Override
    public boolean test(Object other) {
      Long otherLong;
      if(other instanceof Integer) otherLong = new Long((Integer) other);
      else otherLong = (Long) other;
      if(isIncluding()) {
        return otherLong <= getFrom() && otherLong >= getTo();
      } else {
        return otherLong < getFrom() && otherLong > getTo();
      }
    }
  }
}
