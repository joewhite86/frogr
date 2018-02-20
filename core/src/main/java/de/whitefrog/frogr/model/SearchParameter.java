package de.whitefrog.frogr.model;

import com.fasterxml.jackson.annotation.*;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

@XmlRootElement
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY,
  getterVisibility = JsonAutoDetect.Visibility.NONE,
  setterVisibility = JsonAutoDetect.Visibility.NONE)
public class SearchParameter implements Serializable {
  public static final int DefaultLimit = 10;
  public static final Locale DefaultLocale = Locale.GERMAN;

  public enum SortOrder {ASC, DESC}
  
  private String query;
  private Integer limit;
  private Integer page;
  private Integer start;
  private Integer depth;
  private Boolean count;
  private Locale locale;
  private Set<Long> ids = new HashSet<>();
  private Set<String> uuids = new HashSet<>();
  private List<Filter> filters = new ArrayList<>();
  private List<OrderBy> orderBy = new ArrayList<>();
  private FieldList fields = new FieldList();
  private List<String> returns = new ArrayList<>();
  
  public SearchParameter clone() {
    SearchParameter clone = new SearchParameter();
    clone.page = page;
    clone.limit = limit;
    clone.start = start;
    clone.query = query;
    clone.depth = depth;
    clone.count = count;
    clone.locale = locale;
    clone.ids = ids;
    clone.uuids = uuids;
    clone.filters = new ArrayList<>(filters());
    clone.orderBy = new ArrayList<>(orderBy());
    clone.fields = new FieldList();
    clone.fields.addAll(fields);
    clone.returns = returns;
    return clone;
  }

  public SearchParameter() {
  }

  public SearchParameter(int limit) {
    this.limit = limit;
  }

  public SearchParameter(int page, int limit) {
    if(page <= 0) throw new IllegalArgumentException("page cannot be equal or less than 0");
    this.page = page;
    this.limit = limit;
    this.start = (page-1) * limit;
  }

  public boolean count() {
    return count != null && count;
  }

  public SearchParameter count(boolean count) {
    this.count = count;
    return this;
  }

  public boolean containsFilter(String property) {
    for(Filter filter : filters()) {
      if(filter.getProperty().equals(property)) return true;
    }
    return false;
  }

  public SearchParameter fields(String... fields) {
    for(String field: fields) {
      this.fields.add(new QueryField(field));
    }
    return this;
  }

  public SearchParameter fields(QueryField... fields) {
    Collections.addAll(this.fields, fields);
    return this;
  }

  public SearchParameter fields(FieldList fields) {
    this.fields = fields;
    return this;
  }

  public List<String> fields() {
    return this.fields.stream().map(QueryField::field).collect(Collectors.toList());
  }

  public FieldList fieldList() {
    return fields;
  }

  public SearchParameter locale(Locale locale) {
    this.locale = locale;
    return this;
  }

  public Locale locale() {
    return locale != null? locale: DefaultLocale;
  }

  @JsonProperty("q")
  public String query() {
    return query;
  }

  @JsonProperty("q")
  public SearchParameter query(String query) {
    this.query = query;
    return this;
  }

  @JsonIgnore
  public boolean fetch() {
    return !fields().isEmpty();
  }

  public Collection<Filter> filters() {
    return filters;
  }

  public List<Filter> getFilter(String property) {
    List<Filter> filters = new ArrayList<>();
    for(Filter filter : filters()) {
      if(filter.getProperty().equals(property)) filters.add(filter);
    }
    return filters;
  }

  public SearchParameter filter(String property, String value) {
    filters.add(new Filter.Equals(property, value));
    return this;
  }

  public SearchParameter filter(Filter filter) {
    filters.add(filter);
    return this;
  }

  public SearchParameter removeFilter(String property) {
    filters().removeIf(filter -> filter.getProperty().equalsIgnoreCase(property));
    return this;
  }

  public SearchParameter start(int start) {
    this.start = start;
    if(limit != null) this.page = Math.round(start / limit) + 1;
    return this;
  }

  public int start() {
    return start != null? start: 0;
  }

  @JsonIgnore
  public boolean isFiltered() {
    return !filters.isEmpty();
  }

  public List<Long> ids() {
    return new ArrayList<>(ids);
  }

  public SearchParameter ids(Long... ids) {
    this.ids.addAll(Arrays.asList(ids));
    return this;
  }

  public SearchParameter ids(Collection<Long> ids) {
    this.ids = new HashSet<>(ids);
    return this;
  }

  public List<String> uuids() {
    return new ArrayList<>(uuids);
  }

  public SearchParameter uuids(String... uuids) {
    this.uuids.addAll(Arrays.asList(uuids));
    return this;
  }

  public SearchParameter uuids(Collection<String> uuids) {
    this.uuids = new HashSet<>(uuids);
    return this;
  }

  public int limit() {
    return limit != null? limit: DefaultLimit;
  }

  public SearchParameter limit(int limit) {
    this.limit = limit;
    if(start != null) this.page = Math.round(start / limit) + 1;
    return this;
  }

  public int page() {
    return page != null? page: 1;
  }

  public SearchParameter page(int page) {
    this.page = page;
    if(limit != null) this.start = (page-1) * limit;
    return this;
  }

  public SearchParameter incrementPage() {
    page(page()+1);
    return this;
  }

  public List<OrderBy> orderBy() {
    return orderBy;
  }

  public SearchParameter orderBy(String field) {
    return orderBy(field, SortOrder.ASC);
  }

  public SearchParameter orderBy(String field, SortOrder dir) {
    orderBy.add(new OrderBy(field, dir));
    return this;
  }

  @JsonIgnore
  public boolean isOrdered() {
    return !orderBy.isEmpty();
  }
  
  public SearchParameter returns(String... fields) {
    this.returns.addAll(Arrays.asList(fields));
    return this;
  }
  
  public List<String> returns() {
    return this.returns;
  }

  @Override
  public String toString() {
    return new ReflectionToStringBuilder(this).toString();
  }


  @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.NONE)
  public static class OrderBy {
    private String field;
    private String dir = SortOrder.ASC.name();

    @JsonCreator
    public OrderBy(@JsonProperty("field") String field, @JsonProperty("dir") String dir) {
      this.field = field;
      this.dir = dir;
    }

    public OrderBy(String field, SortOrder dir) {
      this(field, dir.name());
    }

    public String field() {
      return field;
    }

    public String dir() {
      return dir;
    }
  }
}