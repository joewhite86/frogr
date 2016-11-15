package de.whitefrog.neobase.rest.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.whitefrog.neobase.model.Base;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single REST response.
 * Will most likely get de-serialized from json.
 */
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class ModelResponse<T extends Base> extends ArrayList<T> implements Response {
  private boolean success = false;
  private long total = 0;
  private String message;
  private int pages = 1;
  private int errorCode;

  public ModelResponse() {}

  public boolean isSuccess() {
    return success;
  }

  public void setSuccess(boolean success) {
    this.success = success;
  }

  @JsonProperty("total")
  public long total() {
    return total;
  }

  public void setTotal(long total) {
    this.total = total;
  }

  @JsonProperty("data")
  public List<T> data() {
    return this;
  }

  public void setData(List<T> data) {
    clear();
    addAll(data);
    if(this.total == 0) this.total = size();
  }

  @JsonIgnore
  public T singleton() { return isEmpty()? null: get(0); }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  @JsonProperty("pages")
  public int pages() {
    return pages;
  }

  public void setPages(int pages) {
    this.pages = pages;
  }

  @JsonProperty("errorCode")
  public int errorCode() {
    return errorCode;
  }

  public void setErrorCode(int errorCode) {
    this.errorCode = errorCode;
  }
}
