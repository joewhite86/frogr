package de.whitefrog.neobase.model.rest;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.function.Predicate;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@JsonSubTypes({
  @JsonSubTypes.Type(value = Filter.Equals.class),
  @JsonSubTypes.Type(value = Filter.LessThan.class),
  @JsonSubTypes.Type(value = Filter.GreaterThan.class),
  @JsonSubTypes.Type(value = Filter.Range.class)
})
public interface Filter extends Predicate<Object> {
    Object getValue();

    void setValue(Object value);

    class Equals implements Filter, Predicate<Object> {
        private Object value;

        public Equals() {
        }

        public Equals(Object value) {
            this.value = value;
        }

        public Object getValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = value;
        }

        @Override
        public boolean test(Object other) {
            return other.equals(getValue());
        }
    }

    class NotEquals implements Filter, Predicate<Object> {
        private Object value;

        public NotEquals() {
        }

        public NotEquals(Object value) {
            this.value = value;
        }

        public Object getValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = value;
        }

        @Override
        public boolean test(Object other) {
            return !other.equals(getValue());
        }
    }

    class GreaterThan implements Filter, Predicate<Object> {
        private boolean including = false;
        private long value;

        public GreaterThan() {
        }

        public GreaterThan(long value) {
            this.value = value;
        }

        public Long getValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = (long) value;
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

    class LessThan implements Filter, Predicate<Object> {
        private boolean including = false;
        private long value;

        public LessThan() {
        }

        public LessThan(long value) {
            this.value = value;
        }

        public Long getValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = (long) value;
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

    class Range implements Filter, Predicate<Object> {
        private boolean including = true;
        private long from;
        private long to;

        public Range() {
        }

        public Range(long from, long to) {
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
