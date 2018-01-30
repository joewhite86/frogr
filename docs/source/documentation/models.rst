Models
======

Models define the data structure, used for our project.
Each model has at least extend the ``Model`` interface.

Entities
--------
 
For entities there's a base class ``Entity`` that implements all needed methods for us.

Entity models primarily consists of field and relationship definitions that define how it is used inside our project.
I recommend using Kotlin_ for models, because I'm lazy and hate to write getter and setter methods, but thats up to you ;)

Relationships
-------------

For relationships we can use the ``BaseRelationship<From, To>`` class.

We can use every annotated field entities are using, except ``@RelatedTo`` and ``@RelationshipCount``.

Fields
------

Model fields should always be initialized with ``null``, so that the persistence layer can properly decide if 
the value is relevant for storing to database. Also we should not use primitive types here.

Fields can be annotated with hibernate annotations extended by a set of unique ones. These are:

@Fetch
  Indicates that a field should be automatically fetched.

@Indexed 
  Indicates that a field should be handled by an index.

@Lazy
  Indicator for lists to fetch them lazily on demand, not every list item at once.
  Using this will NOT delete relationships when one is missing in save operations.
  We have to delete them manually, when needed.

@NotPersistant
  The field should not be persisted.

@NullRemove
  Remove a property if set to ``null``.

**@RelatedTo(type=None, direction=Direction.OUTGOING, multiple=false, restrictType=false)**

| The field represents a relation to another or the same model. 
| ``type`` has to be set to the relationship type name.
| ``direction`` defaults to an outgoing relationship, but can be also incoming or even both.
| ``multiple`` allows multiple relationships to the same model.
| ``restrictType`` restricts the type. Used when the same relationship type is used for multiple model relationships.
|

**@RelationshipCount(type=None, direction=Direction.OUTGOING, otherModel=Model.class)**

| The field should contain the relationship count for a specified relationship type when fetched. Will not be persisted.
| ``type`` has to be set to the relationship type name.
| ``direction`` defaults to an outgoing relationship, but can be also incoming or even both.
| ``otherModel`` is used to only query for specific models.
|

@Required
  The field is required upon storing to database. If it is missing an exception will be thrown.

@Unique
  The field is unique across all models of the same type. Will be indexed as well. 
  If a duplicate value is passed an exception will be thrown.

@Uuid
  Auto-generated uuid field. This should not be required as theres always an uuid field on each model.

.. _Kotlin: https://kotlinlang.org
