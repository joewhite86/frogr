package de.whitefrog.frogr.persistence

import de.whitefrog.frogr.Service
import de.whitefrog.frogr.exception.*
import de.whitefrog.frogr.model.*
import de.whitefrog.frogr.model.annotation.RelatedTo
import de.whitefrog.frogr.repository.DefaultRelationshipRepository
import de.whitefrog.frogr.repository.ModelRepository
import de.whitefrog.frogr.repository.RelationshipRepository
import org.apache.commons.lang.Validate
import org.neo4j.graphdb.*
import org.slf4j.LoggerFactory
import java.util.*
import de.whitefrog.frogr.model.relationship.Relationship as FRelationship

/**
 * Handles most persistent operations required for neo4j to deal with frogr relationships.
 * Some relationship operations are kept in the persistence class.
 */
class Relationships internal constructor(private val service: Service, private val persistence: Persistence) {
  companion object {
    private val logger = LoggerFactory.getLogger(Relationships::class.java)
  }

  /**
   * Adds a relationship to a existing node. Tests for multiple relationships and a persisted foreign node.
   */
  private fun <T : Model> addRelationship(model: T, node: Node, annotation: RelatedTo, _foreignModel: Model) {
    var foreignModel = _foreignModel
    if (foreignModel.id == -1L) {
      if (foreignModel is FBase && foreignModel.uuid != null) {
        foreignModel = service.repository(foreignModel.javaClass).findByUuid(foreignModel.uuid) as Model
      } else {
        throw RelatedNotPersistedException(
          "the related field $foreignModel is not yet persisted")
      }
    }
    val foreignNode = persistence.getNode(foreignModel)
    val relationshipType = RelationshipType.withName(annotation.type)
    if (!annotation.multiple && hasRelationshipTo(node, foreignNode, relationshipType, annotation.direction)) {
      // the relationship already exists, no more work to do
      return
    }
    var repository: RelationshipRepository<FRelationship<Model, Model>>
    try {
      repository = service.repository(relationshipType.name())
    } catch (e: RepositoryNotFoundException) {
      // if no repository is found in the RepositoryFactory, create a new one here
      // TODO: this should be handled inside of RepositoryFactory but then it would  
      // TODO: create a DefaultRealtionshipRepository for each unknown type name passed 
      // TODO: and i haven't found a solution yet
      repository = DefaultRelationshipRepository(relationshipType.name())
      try {
        service.repositoryFactory().setRepositoryService(repository)
        service.repositoryFactory().register(relationshipType.name(), repository)
      } catch (ex: ReflectiveOperationException) {
        throw RepositoryInstantiationException(ex.cause!!)
      }

    }

    val relationship: FRelationship<Model, Model>

    relationship = when {
      annotation.direction == Direction.OUTGOING -> 
        repository.createModel(model, foreignModel)
      annotation.direction == Direction.INCOMING -> 
        repository.createModel(foreignModel, model)
      else -> // on Direction.BOTH we can create either a OUTGOING or an INCOMING relation
        repository.createModel(model, foreignModel)
    }
    repository.save(relationship)
  }

  fun <T : FRelationship<*, *>> getRelationshipBetween(model: Model, other: Model,
                                                       type: RelationshipType, dir: Direction): T? {
    val relationship = getRelationshipBetween(persistence.getNode(model), persistence.getNode(other), type, dir)
    return if (relationship == null) null else persistence[relationship]
  }

  fun getRelationshipBetween(node: Node, other: Node, type: RelationshipType, dir: Direction): Relationship? {
    val relationships = node.getRelationships(dir, type)
    return relationships.firstOrNull { it.getOtherNode(node) == other }
  }

  /**
   * Get a single related model
   * @param model Model that contains the relationship
   * @param annotation RelatedTo annotation
   * @param fields Fields that should get fetched for the related model
   * @return The related model or null when none exists
   */
  internal fun getRelatedModel(model: Model, annotation: RelatedTo, fields: FieldList): Model? {
    Validate.notNull(model)
    Validate.notNull(annotation.type)

    try {
      val relationship = persistence.getNode(model).getSingleRelationship(
        RelationshipType.withName(annotation.type), annotation.direction)
      if (relationship != null) {
        val node = persistence.getNode(model)
        val other = relationship.getOtherNode(node)
        val type = other.getProperty(Model.Type) as String
        val repository = service.repository<ModelRepository<Model>>(type)
        return repository.createModel(other, fields)
      }
    } catch (e: NotFoundException) {
      if (e.message!!.startsWith("More than")) {
        logger.error(e.message)
        logger.error("Relationships are:")
        persistence.getNode(model).getRelationships(
          RelationshipType.withName(annotation.type), annotation.direction)
          .forEach { rel -> logger.error(rel.toString()) }
        throw e
      }
    }

    return null
  }

  internal fun <M : Model> getRelatedModels(model: Model, descriptor: FieldDescriptor<*>,
                                            queryField: QueryField, fields: FieldList): Set<M> {
    val annotation = descriptor.annotations().relatedTo!!
    Validate.notNull(model)
    Validate.notNull(annotation.type)

    val iterator = persistence.getNode(model).getRelationships(
      annotation.direction, RelationshipType.withName(annotation.type)).iterator() as ResourceIterator<Relationship>

    val models = HashSet<M>()
    val node = persistence.getNode(model)
    var count: Long = 0

    while (iterator.hasNext()) {
      if (count < queryField.skip()) {
        iterator.next()
        count++
        continue
      }
      if (count++ == (queryField.skip() + queryField.limit()).toLong()) break

      val relationship = iterator.next()
      val other = relationship.getOtherNode(node)
      val type = other.getProperty(Model.Type) as String
      if (annotation.restrictType && type != persistence.cache().getModelName(descriptor.baseClass())) {
        count--
        continue
      }
      val repository = service.repository<ModelRepository<M>>(type)
      models.add(repository.createModel(other, fields))
    }
    iterator.close()
    return models
  }

  /**
   * Get the neo4j relationship from a model. A id has to be set.
   * @param relationship The relationship model
   * @return The corresponding neo4j relationship
   */
  fun <R : FRelationship<*, *>> getRelationship(relationship: R): Relationship {
    return if (relationship.id > -1) {
      service.graph().getRelationshipById(relationship.id)
    } else {
      throw UnsupportedOperationException("cant find relationship without id")
    }
  }

  /**
   * Get all relationships matching a model's field descriptor.
   * For all relationships fetch the fields passed.
   *
   * @param model Model that contains the relationships
   * @param descriptor Descriptor for the relationship field
   * @param fields Field list to fetch for the relationships
   * @return Set of matching relationships
   * descriptor could not be created
   */
  internal fun <R : FRelationship<*, *>> getRelationship(model: Model, descriptor: FieldDescriptor<*>,
                                                         fields: FieldList): R? {
    val annotation = descriptor.annotations().relatedTo!!
    Validate.notNull(model)
    Validate.notNull(annotation.type)

    val iterator = persistence.getNode(model).getRelationships(
      annotation.direction, RelationshipType.withName(annotation.type)).iterator() as ResourceIterator<Relationship>

    var relationshipModel: R? = null
    if (iterator.hasNext()) {
      val relationship = iterator.next()
      relationshipModel = persistence[relationship, fields]
    }
    iterator.close()
    return relationshipModel
  }

  /**
   * Get all relationships matching a model's field descriptor.
   * For all relationships fetch the fields passed.
   *
   * @param model Model that contains the relationships
   * @param descriptor Descriptor for the relationship field
   * @param queryField QueryField used for paging
   * @param fields Field list to fetch for the relationships
   * @return Set of matching relationships
   * descriptor could not be created
   */
  internal fun <R : FRelationship<*, *>> getRelationships(model: Model, descriptor: FieldDescriptor<*>,
                                                          queryField: QueryField?, fields: FieldList): Set<R> {
    val annotation = descriptor.annotations().relatedTo!!
    Validate.notNull(model)
    Validate.notNull(annotation.type)

    val iterator = persistence.getNode(model).getRelationships(
      annotation.direction, RelationshipType.withName(annotation.type)).iterator() as ResourceIterator<Relationship>

    val models = HashSet<R>()
    var count: Long = 0
    while (iterator.hasNext()) {
      if (queryField != null && count < queryField.skip()) {
        iterator.next()
        count++
        continue
      }
      if (queryField != null && count++ == (queryField.skip() + queryField.limit()).toLong()) break

      val relationship = iterator.next()
      models.add(persistence[relationship, fields])
    }
    iterator.close()
    return models
  }

  /**
   * Tests if a particular node has a specific relationship to another one.
   *
   * @param node  The node from where the relationship should start.
   * @param other The node, which should be related.
   * @param type  The relationship type.
   * @return true if a relationship exists, otherwise false.
   */
  fun hasRelationshipTo(node: Node, other: Node, type: RelationshipType, direction: Direction): Boolean {
    val relationships = node.getRelationships(direction, type)
    return relationships.any { it.getOtherNode(node) == other }
  }

  /**
   * Save method called from RelationshipRepository's
   */
  fun <T : FRelationship<*, *>> save(context: SaveContext<T>): T {
    val model = context.model()
    var create = false

    if (!model.isPersisted) {
      create = true

      if (!model.from.isPersisted)
        throw FrogrException("the model " + model.from + " is not yet persisted, but used as 'from' in relationship " + model)
      if (!model.to.isPersisted)
        throw FrogrException("the model " + model.to + " is not yet persisted, but used as 'to' in relationship " + model)

      val fromNode = persistence.getNode(model.from)
      val toNode = persistence.getNode(model.to)
      val relType = RelationshipType.withName(context.repository().type)
      val relationship = fromNode.createRelationshipTo(toNode, relType)
      context.setNode(relationship)
      model.id = relationship.id
      if(model is FBase) model.created = System.currentTimeMillis()
    } else {
      if(model is FBase) model.updateLastModified()
    }

    for (property in model.removeProperties) {
      removeProperty(model, property)
    }
    // clone all properties from model
    for (field in context.fieldMap()) {
      persistence.saveField(context, field, create)
    }
    model.checkedFields.clear()

    if (logger.isInfoEnabled) {
      logger.info("Relationship {}({}, {}) {}", persistence.cache().getModelName(model.javaClass), model.from, model.to,
        if (create) "created" else "updated")
    }

    return model
  }

  private fun <T : FRelationship<*, *>> save(model: Model, relModel: T, annotation: RelatedTo) {
    if (!relModel.from.isPersisted) {
      throw RelatedNotPersistedException(
        "the 'from' model " + relModel.from + " (" + annotation.type + ") is not yet persisted")
    }
    if (!relModel.to.isPersisted) {
      throw RelatedNotPersistedException(
        "the 'to' model " + relModel.to + " (" + annotation.type + ") is not yet persisted")
    }

    val repository = service.repository(relModel.javaClass)

    if (annotation.direction == Direction.INCOMING) {
      if (relModel.to != model) {
        throw PersistException(relModel.toString() + " should have " + model + " as 'to' field set")
      }
    } else if (annotation.direction == Direction.OUTGOING) {
      if (relModel.from != model) {
        throw PersistException(relModel.toString() + " should have " + model + " as 'from' field set")
      }
    } else if (annotation.direction == Direction.BOTH) {
      if (relModel.from != model && relModel.to != model) {
        throw PersistException(relModel.toString() + "should have " + model + " either set as 'from' or 'to' field")
      }
    }
    repository.save(relModel)
  }

  /**
   * Used only from persistence class with the models save context and field descriptor.
   */
  @Throws(IllegalAccessException::class)
  @Suppress("UNCHECKED_CAST")
  internal fun <T : Model> saveField(context: SaveContext<T>, descriptor: FieldDescriptor<*>) {
    val annotations = descriptor.annotations()
    val model = context.model()
    val node = context.node<Node>()
    val relatedTo = annotations.relatedTo!!
    val value = descriptor.field().get(model)
    // Handle single relationships
    if (!descriptor.isCollection) {
      val existing = node.getSingleRelationship(
        RelationshipType.withName(relatedTo.type), relatedTo.direction)
      existing?.delete()

      if (descriptor.isModel) {
        val foreignModel = value as Model
        addRelationship(model, node, relatedTo, foreignModel)
      } else {
        val relModel = value as FRelationship<*, *>
        save(model, relModel, relatedTo)
      }
    } else {
      val collection = value as Collection<*>
      // check if relationship is obsolete and delete if neccessary
      if (!annotations.lazy) {
        val relationshipType = RelationshipType.withName(relatedTo.type)
        for (relationship in node.getRelationships(relatedTo.direction, relationshipType)) {
          val other = persistence.get<Base>(relationship.getOtherNode(node))
          if (!collection.contains(other)) {
            relationship.delete()
            logger.info("{}: relationship to {} removed", model, other)
          }
        }
      }
      // Handle collection of models
      // add the relationship if the foreign model is persisted
      if (descriptor.isModel) {
        for (foreignModel in value as Collection<Model>) {
          addRelationship(model, node, relatedTo, foreignModel)
        }
      } else {
        for (relModel in value as Collection<FRelationship<*, *>>) {
          save(model, relModel, relatedTo)
        }
      }// Handle collections of relationship models
    }// Handle collections
  }

  /**
   * Removes a property inside the graph and on the model.
   *
   * @param model Model to remove the property from
   * @param property Property name to remove
   */
  fun removeProperty(model: FRelationship<*, *>, property: String) {
    val node = getRelationship(model)
    node.removeProperty(property)
    try {
      val field = model.javaClass.getDeclaredField(property)
      if (!field.isAccessible) field.isAccessible = true
      field.set(model, null)
    } catch (e: ReflectiveOperationException) {
      throw FrogrException("field $property could not be found on $model", e)
    }

  }

  /**
   * Used in BaseRelationshipRepository to delete an entire relationship
   * @param relationship Relationship model to delete
   */
  fun <R : FRelationship<*, *>> delete(relationship: R) {
    val neoRel = getRelationship(relationship)
    neoRel.delete()
    logger.info("relationship {} between {} and {} removed", neoRel.type, relationship.from, relationship.to)
  }

  fun <T : Model> delete(model: T, type: RelationshipType, direction: Direction, _foreignModel: Model) {
    var foreignModel = _foreignModel
    if(foreignModel.id == -1L) {
      if(foreignModel is FBase && foreignModel.uuid != null) {
        foreignModel = service.repository(foreignModel.javaClass).findByUuid(foreignModel.uuid) as Model
      } else {
        throw RelatedNotPersistedException(
          "the related field $foreignModel is not yet persisted")
      }
    }
    val node = persistence.getNode(model)
    val foreignNode = persistence.getNode(foreignModel)
    for (relationship in node.getRelationships(type, direction)) {
      if (relationship.getOtherNode(node) == foreignNode) {
        relationship.delete()
        logger.info("relationship {} between {} and {} removed",
          type.name(), model, foreignModel)
      }
    }
  }
}
