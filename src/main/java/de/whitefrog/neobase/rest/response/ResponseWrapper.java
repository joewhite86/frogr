package de.whitefrog.neobase.rest.response;


import java.util.List;

public class ResponseWrapper {
  public boolean success;
  public long total;
  public String message;
  public Integer errorCode;
  public int pages;
  public List data;

  public ResponseWrapper(Response response) {
    success = response.isSuccess();
    total = response.total();
    message = response.getMessage();
    if(response.errorCode() != 0) errorCode = response.errorCode();
    pages = response.pages();
    data = response.data();
  }
}
