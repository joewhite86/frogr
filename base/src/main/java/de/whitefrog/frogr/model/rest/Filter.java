package de.whitefrog.frogr.model.rest;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

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
  
  abstract class Default implements Filter {
    private Object value;
    private String property;
    
    public Default() {}
    public Default(String property, Object value) {
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
    public Equals() {
      super();
    }
    public Equals(String property, Object value) {
      super(property, value);
    }

    @Override
      public boolean test(Object other) {
          return other.equals(getValue());
      }
  }

  class NotEquals extends Default implements Filter, Predicate<Object> {
    public NotEquals() {
      super();
    }

    public NotEquals(String property, Object value) {
      super(property, value);
    }

    @Override
      public boolean test(Object other) {
          return !other.equals(getValue());
      }
  }

  class GreaterThan extends Default implements Filter, Predicate<Object> {
      private boolean including = false;

    public GreaterThan() {
      super();
    }

    public GreaterThan(String property, Object value) {
      super(property, value);
    }

    @Override
      public Long getValue() {
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

    public LessThan() {
      super();
    }

    public LessThan(String property, Object value) {
      super(property, value);
    }

    public Long getValue() {
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
    public StartsWith() {}
    public StartsWith(String property, String value) {
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
    public EndsWith() {}
    public EndsWith(String property, String value) {
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
    public Contains() {}
    public Contains(String property, String value) {
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

      public Range() {
      }

      public Range(String property, long from, long to) {
          super(property, from);
          this.from = from;
          this.to = to;
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
