package de.whitefrog.neobase.rest.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.List;

public interface Response extends Serializable {
  boolean isSuccess();

  @JsonProperty("total")
  long total();

  @JsonProperty("data")
  List data();

  String getMessage();

  @JsonProperty("pages")
  int pages();

  @JsonProperty("errorCode")
  int errorCode();
}
