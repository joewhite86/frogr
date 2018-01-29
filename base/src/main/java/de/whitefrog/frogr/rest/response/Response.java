package de.whitefrog.frogr.rest.response;

import org.apache.commons.collections.CollectionUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Model to format responses in a unified way.
 */
public class Response<T> implements Serializable {
  private boolean success;
  private Long total;
  private String message;
  private Integer errorCode;
  private Integer pages;
  private List<T> data = new ArrayList<>();
  
  public void add(T... data) {
    CollectionUtils.addAll(this.data, data);
  }
  
  public Object singleton() {
    return data.isEmpty()? null: data.get(0);
  }

  public boolean isSuccess() {
    return success;
  }

  public void setSuccess(boolean success) {
    this.success = success;
  }

  public Long getTotal() {
    return total;
  }

  public void setTotal(Long total) {
    this.total = total;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public Integer getErrorCode() {
    return errorCode;
  }

  public void setErrorCode(Integer errorCode) {
    this.errorCode = errorCode;
  }

  public Integer getPages() {
    return pages;
  }

  public void setPages(Integer pages) {
    this.pages = pages;
  }

  public List<T> getData() {
    return data;
  }

  public void setData(List<T> data) {
    this.data = data;
  }
}
