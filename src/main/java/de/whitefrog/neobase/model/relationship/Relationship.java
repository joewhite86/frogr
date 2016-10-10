package de.whitefrog.neobase.model.relationship;

import de.whitefrog.neobase.model.Base;

public interface Relationship<From, To> extends Base {
  From getFrom();
  Relationship setFrom(From from);

  To getTo();
  Relationship setTo(To to);
}
