package de.whitefrog.frogr.persistence

import com.fasterxml.uuid.Generators
import de.whitefrog.frogr.Service
import de.whitefrog.frogr.exception.*
import de.whitefrog.frogr.helper.KotlinHelper
import de.whitefrog.frogr.helper.ReflectionUtil
import de.whitefrog.frogr.model.*
import de.whitefrog.frogr.model.Entity
import de.whitefrog.frogr.model.annotation.IndexType
import de.whitefrog.frogr.model.relationship.BaseRelationship
import de.whitefrog.frogr.repository.ModelRepository
import org.apache.commons.lang.Validate
import org.apache.commons.lang.reflect.ConstructorUtils
import org.neo4j.graphdb.*
import org.neo4j.helpers.collection.Iterables
import org.slf4j.LoggerFactory
import java.util.*

/**
 * The link between the models and repositories and neo4j itself.
 * Handles every persitent operation required for neo4j to deal with the models and relationships
 * used in frogr. For Relationships the operations are moved to the Relationships class but
 * some operations common to nodes and relationships are kept here.
 */
class Persistence(private val service: Service, private val cache: ModelCache) {
  companion object {
    private val logger = LoggerFactory.getLogger(Persistence::class.java)
  }
  
  private val relationships: Relationships = Relationships(service, this)
  private val uuidGenerator = Generators.timeBasedGenerator()

  fun cache(): ModelCache {
    return cache
  }

  fun relationships(): Relationships {
    return relationships
  }

  /**
   * Delete a model from repository.
   *
   * @param model      The model to delete.
   */
  fun <T : Model> delete(model: T) {
    val node = getNode(model)

    for (relationship in node.relationships) {
      relationship.delete()
    }

    // delete node
    node.delete()
  }

  /**
   * Save the specified model.
   *
   * @param repository Repository to saveField the model in.
   * @param context      The model to saveField.
   * @return The saved model.
   * @throws MissingRequiredException A required field is missing.
   */
  @Throws(MissingRequiredException::class)
  fun <T : Model> save(repository: ModelRepository<T>, context: SaveContext<T>): T {
    val model = context.model()
    val label = repository.label()
    var create = false

    if (!model.isPersisted) {
      create = true
      val node = service.graph().createNode(label)
      context.setNode(node)
      // add labels defined in repository
      repository.labels().stream()
        .filter { l -> !node.hasLabel(l) }
        .forEach({ node.addLabel(it) })
      model.id = node.id
      model.created = System.currentTimeMillis()
      model.type = label.name()
    } else {
      if (model.type == null) model.type = label.name()
      model.updateLastModified()
    }

    for (property in context.model().removeProperties) {
      removeProperty(context.model(), property)
    }
    // clone all properties from model
    for (field in context.fieldMap()) {
      saveField(context, field, create)
    }
    model.checkedFields.clear()

    logger.info("{} {}", model, if (create) "created" else "updated")

    return model
  }

  @Suppress("UNCHECKED_CAST")
  internal fun <T : Base> saveField(context: SaveContext<T>, descriptor: FieldDescriptor<*>, created: Boolean) {
    val field = descriptor.field()
    val annotations = descriptor.annotations()
    val model = context.model()
    val node = context.node<PropertyContainer>()

    var value: Any? = null
    try {
      value = field.get(model)

      // when the annotation @Required is present, a value is expected
      if (created && annotations.required && (value == null || value is String && value.isEmpty())) {
        throw MissingRequiredException(model, field)
      }

      var valueChanged = created || context.fieldChanged(field.name)

      if (!annotations.notPersistent && !annotations.blob) {
        // Generate an uuid when the value is actually null
        if (created && annotations.uuid && field.get(model) == null) {
          val uuid = generateUuid()
          field.set(model, uuid)
          value = uuid
          valueChanged = true
        }

        if (value != null) {
          // handle relationships
          if (annotations.relatedTo != null && valueChanged && model is Model) {
            relationships.saveField(context as SaveContext<Model>, descriptor)
          } else if (value !is Collection<*> && value !is Model) {
            // store enum names
            if (value.javaClass.isEnum) {
              if (!node.hasProperty(field.name) || (value as Enum<*>).name != node.getProperty(field.name)) {
                node.setProperty(field.name, (value as Enum<*>).name)
                logger.info("{}: set enum value \"{}\" to \"{}\"", model, field.name, value.name)
              }
            } else if (value is Date) {
              node.setProperty(field.name, value.time)
              logger.info("{}: set date value \"{}\" to \"{}\"", model, field.name, value.time)
            } else if (valueChanged) {
              node.setProperty(field.name, value)
              logger.info("{}: set value \"{}\" to \"{}\"", model, field.name, value)
              if (value is String && annotations.indexed?.type == IndexType.LowerCase) {
                node.setProperty(field.name + "_lower", value.toLowerCase())
                logger.info("{}: set value \"{}\" to \"{}\"", model, field.name + "_lower", value.toLowerCase())
              }
            }// store all other values
            // store dates as timestamp
          }// Handle other values
        } else if (valueChanged && annotations.nullRemove) {
          logger.info("{}: removed value \"{}\"", model, field.name)
          node.removeProperty(field.name)
          if (annotations.indexed?.type == IndexType.LowerCase) {
            node.removeProperty(field.name + "_lower")
          }
        }// if the new value is null and @NullRemove is set on the field,
        // we need to remove the property from the node and the index
      }
    } catch (e: ReflectiveOperationException) {
      logger.error("Could not get property on {}: {}", model, e.message, e)
    } catch (e: ConstraintViolationException) {
      throw DuplicateEntryException("A " + model.javaClass.simpleName.toLowerCase() + " with the " +
        field.name + " \"" + value + "\" already exists", model, field)
    } catch (e: IllegalArgumentException) {
      logger.error("Could not store property {} on {}: {}", field.name, model, e.message)
    }

  }

  /**
   * Get a model instance from a neo node.
   *
   * @param node Node to create the model from
   * @return The created model
   * @throws PersistException Is thrown if a field can not be converted
   */
  @Throws(PersistException::class)
  operator fun <T : Base> get(node: PropertyContainer): T {
    return get(node, FieldList())
  }

  /**
   * Get a model instance from a neo4j node.
   *
   * @param node Node to create the model from
   * @return The created model
   * @throws PersistException Is thrown if a field can not be converted
   */
  @Throws(PersistException::class)
  @Suppress("UNCHECKED_CAST")
  operator fun <T : Base> get(node: PropertyContainer, _fields: FieldList): T {
    var fields = _fields
    Validate.notNull(node, "node can't be null")
    try {
      var clazz: Class<T>? = getClass(node) as Class<T>
      if (clazz == null) {
        // choose basic classes when there is none defined
        clazz = if (node is Node) Model::class.java as Class<T> else BaseRelationship::class.java as Class<T>
      }
      val model: T
      if (node is Node) {
        model = clazz.newInstance()
        model.id = node.id
      } else {
        val rel = node as Relationship
        val from = get<Model>(rel.startNode, fields.getOrEmpty("from").subFields())
        val to = get<Model>(rel.endNode, fields.getOrEmpty("to").subFields())

        val constructor = ConstructorUtils.getMatchingAccessibleConstructor(clazz, arrayOf<Class<*>>(from.javaClass, to.javaClass))
        model = constructor.newInstance(from, to) as T
        model.id = rel.id
        fields = FieldList(fields)
        fields.remove(QueryField("from"))
        fields.remove(QueryField("to"))
      }
      service.repository(clazz).fetch(model, fields)
      return model
    } catch (e: IllegalStateException) {
      throw e
    } catch (e: Exception) {
      throw e as? PersistException ?: PersistException(e)
    }

  }

  /**
   * Get the model class for a node or relationship.
   * The "model" property will be preferred, else "type" will be taken for evaluation.
   *
   * @param node Node or relationship to get the corresponding class for
   * @return The correct model class for the node or relationship passed
   */
  private fun getClass(node: PropertyContainer): Class<*>? {
    val className = if (node is Relationship) {
      node.type.name()
    } else {
      node.getProperty(if (node.hasProperty(Model.Model)) Entity.Model else Entity.Type) as String
    }
    return cache().getModel(className)
  }

  /**
   * Generate a fresh uuid.
   *
   * @return Generated uuid.
   */
  private fun generateUuid(): String {
    val uuid = uuidGenerator.generate()
    return java.lang.Long.toHexString(uuid.mostSignificantBits) + java.lang.Long.toHexString(uuid.leastSignificantBits)
  }

  /**
   * Removes a property inside the graph and on the model.
   *
   * @param model Model to remove the property from
   * @param property Property name to remove
   */
  private fun removeProperty(model: Model, property: String) {
    val descriptor = cache().fieldDescriptor(model.javaClass, property) ?: throw FieldNotFoundException(property, model)

    val node = getNode(model)
    node.removeProperty(property)
    if (descriptor.annotations().indexed?.type == IndexType.LowerCase) {
      node.removeProperty(property + "_lower")
    }
    try {
      val field = ReflectionUtil.getSuperField(model.javaClass, property)
      if (!field.isAccessible) field.isAccessible = true
      field.set(model, null)
    } catch (e: ReflectiveOperationException) {
      throw FieldNotFoundException(property, model)
    }

  }

  /**
   * Get the neo4j node for a particular model using its id.
   *
   * @param model Model to search inside the graph
   * @return The neo4j node equivalent for the passed model
   */
  fun getNode(model: Model): Node {
    Validate.notNull(model)
    return when {
      model.id > -1 ->
        service.graph().getNodeById(model.id)
      model.uuid != null && model.type != null -> {
        val node = service.graph().findNode(Label.label(model.type!!), Entity.Uuid, model.uuid)
        model.id = node.id
        node
      } 
      else ->
        throw UnsupportedOperationException("cant get a node without id or uuid")
    }
  }

  /**
   * Fetch properties for the passed model from database.
   * This can be either normal properties or relationships if annotated correctly.
   *
   * @param model The model to fetch the properties for
   * @param fields List of fields to fetch
   */
  fun <T : Base> fetch(model: T, vararg fields: String) {
    fetch(model, FieldList.parseFields(Arrays.asList(*fields)), false)
  }

  /**
   * Fetch properties for the passed model from database.
   * This can be either normal properties or relationships if annotated correctly.
   *
   * @param model The model to fetch the properties for
   * @param fields List of fields to fetch as FieldList
   */
  fun <T : Base> fetch(model: T, fields: FieldList) {
    fetch(model, fields, false)
  }

  /**
   * Fetch properties for the passed model from database.
   * This can be either normal properties or relationships if annotated correctly.
   *
   * @param model The model to fetch the properties for
   * @param fields List of fields to fetch as FieldList
   * @param refetch Fetch even if the field was already fetched before
   */
  fun <T : Base> fetch(model: T, fields: FieldList, refetch: Boolean) {
    Validate.notNull(model, "model cannot be null")
    if (!model.isPersisted) throw FrogrException("the model $model is not persisted yet")
    val node: PropertyContainer

    try {
      node = if (model is BaseRelationship<*, *>) {
        val relModel = model as BaseRelationship<*, *>
        relationships.getRelationship(relModel)
      } else {
        getNode(model as Model)
      }

      // iterate over fields to ensure fields with @Fetch annotation and 'allFields' will be fetched
      for (descriptor in cache.fieldMap(model.javaClass)) {
        // don't fetch the 'id' field and fields annotated with @NotPersistent
        if (descriptor.name == "id" && descriptor.annotations().notPersistent) continue

        // always fetch when field is 'type' or 'uuid', or when allFields is set 
        // or the field list contains the current field
        val fetch = descriptor.annotations().fetch ||
          descriptor.name == Entity.Type || descriptor.name == Entity.Uuid ||
          fields.containsField(Entity.AllFields) || fields.containsField(descriptor.name)

        if (fetch) {
          // still fetch if refetch is true or field is not in fetchedFields 
          // or fields does not contiain field or sub-fields is not empty 
          if (!refetch && model.fetchedFields.contains(descriptor.name) &&
            fields.containsField(descriptor.name) && fields[descriptor.name]!!.subFields().isEmpty())
            continue

          fetchField(node, model, descriptor, fields)
        }
      }
    } catch (e: ReflectiveOperationException) {
      logger.error("could not load relations for {}: {}", model, e.message, e)
    }

  }

  @Throws(ReflectiveOperationException::class)
  @Suppress("UNCHECKED_CAST")
  private fun <T : Base> fetchField(node: PropertyContainer, model: T, descriptor: FieldDescriptor<*>,
                                    fields: FieldList) {
    val annotations = descriptor.annotations()
    val field = descriptor.field()
    field.isAccessible = true

    if (node is Relationship) {
      val relModel = model as BaseRelationship<*, *>
      if (field.name == "from" && fields.containsField("from") && !fields["from"]!!.subFields().isEmpty()) {
        service.repository(relModel.from.javaClass).fetch(relModel.from, fields["from"]!!.subFields())
      }
      if (field.name == "to" && fields.containsField("to") && !fields["to"]!!.subFields().isEmpty()) {
        service.repository(relModel.to.javaClass).fetch(relModel.to, fields["to"]!!.subFields())
      }
    }

    // fetch relationship count only
    if (node is Node && annotations.relationshipCount != null && fields.containsField(field.name)) {
      val count = annotations.relationshipCount!!
      field.set(model, Iterables.count(node.getRelationships(count.direction, RelationshipType.withName(count.type))))
    } else if (model is Model && annotations.relatedTo != null) {
      if (!annotations.fetch && !fields.containsField(field.name)) return
      val subFields = if (fields.containsField(field.name)) fields[field.name]!!.subFields() else FieldList()
      val fieldDescriptor = if (fields.containsField(field.name)) fields[field.name]!! else QueryField(field.name)
      // ignore relationships when only AllFields is set
      if (descriptor.isCollection) {
        val related: Collection<*> = if (descriptor.isModel) {
          relationships.getRelatedModels<Model>(model as Model, descriptor, fieldDescriptor, subFields)
        } else {
          relationships.getRelationships<BaseRelationship<Model, Model>>(model as Model, descriptor, fieldDescriptor, subFields)
        }
        field.set(model, if (Set::class.java.isAssignableFrom(field.type)) related else ArrayList(related))
      } else {
        val related: Base? = if (descriptor.isModel) {
          relationships.getRelatedModel(model as Model, annotations.relatedTo!!, subFields)
        } else {
          relationships.getRelationship(model as Model, descriptor, subFields)
        }
        field.set(model, related)
      }
    } 
    else if (node.hasProperty(field.name)) {
      when {
        Enum::class.java.isAssignableFrom(field.type) -> 
          field.set(model, KotlinHelper.getEnumValue(field.type as Class<Enum<*>>, node.getProperty(field.name) as String))
        Date::class.java.isAssignableFrom(field.type) -> 
          field.set(model, Date(node.getProperty(field.name) as Long))
        else -> 
          field.set(model, node.getProperty(field.name))
      }
    }// fetch normal field values
    // fetch related nodes
    model.fetchedFields.add(field.name)
  }

  /**
   * Get a model by label and uuid.
   *
   * @param label The label/type to look for
   * @param uuid The uuid for the model
   * @return The found model or null if none was found
   */
  fun <T : Model> findByUuid(label: String, uuid: String): T? {
    val iterator = service.graph().findNodes(Label.label(label), Entity.Uuid, uuid)
    return if (iterator.hasNext()) get(iterator.next()) else null
  }
}
