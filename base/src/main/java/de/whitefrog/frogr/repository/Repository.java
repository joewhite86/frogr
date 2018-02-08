package de.whitefrog.frogr.repository;

import de.whitefrog.frogr.Service;
import de.whitefrog.frogr.cypher.QueryBuilder;
import de.whitefrog.frogr.model.Base;
import de.whitefrog.frogr.model.SaveContext;
import de.whitefrog.frogr.model.FieldList;
import de.whitefrog.frogr.model.SearchParameter;
import de.whitefrog.frogr.persistence.Persistence;
import de.whitefrog.frogr.persistence.Relationships;
import de.whitefrog.frogr.service.Search;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.PropertyContainer;

import java.util.List;

/**
 * A Repository acts as a link between the models and the database. 
 * It handles all persistent data operations and is easily extensible.
 * @param <T>
 */
public interface Repository<T extends Base> {
  boolean contains(T entity);

  /**
   * Create appropriate the model for a neo4j property container.
   * @param node The property container to use
   * @return The model for the property container
   */
  T createModel(PropertyContainer node);

  /**
   * Create appropriate the model for a neo4j property container and fetch some fields.
   * @param node The property container to use
   * @param fields Field list to fetch from property container
   * @return The model for the property container
   */
  T createModel(PropertyContainer node, FieldList fields);

  /**
   * Dispose method, will be called by the owning service on shutdown.
   */
  void dispose();

  /**
   * Fetch fields from database for a model.
   * @param model The model to use
   * @param fields The fields to fetch
   * @return Updated model instance
   */
  T fetch(Base model, String... fields);

  /**
   * Fetch fields from database for a model, even if already fetched.
   * @param model The model to use
   * @param fields The fields to fetch
   * @return Updated model instance
   */
  T refetch(Base model, String... fields);
  
  /**
   * Fetch fields from database for a model.
   * @param model The model to use
   * @param fields The fields to fetch as FieldList
   * @return Updated model instance
   */
  T fetch(Base model, FieldList fields);

  /**
   * Fetch fields from database for a model, even if already fetched.
   * @param model The model to use
   * @param fields The fields to fetch as FieldList
   * @return Updated model instance
   */
  T refetch(Base model, FieldList fields);

  /**
   * Fetch fields from database for a model.
   * @param model The model to use
   * @param refetch If true, fields will be fetched, even if they were already before
   * @param fields The fields to fetch as FieldList
   * @return Updated model instance
   */
  T fetch(Base model, boolean refetch, FieldList fields);

  /**
   * Find a entity by ID.
   * @param id The id of the entity to look for
   * @param fields Fields to fetch, not required
   * @return The entity if found, otherwise null
   */
  T find(long id, String... fields);

  /**
   * Find a entity by UUID
   * @param uuid The uuid of the entity to look for
   * @param fields Fields to fetch, not required
   * @return The entity if found, otherwise null
   */
  T findByUuid(String uuid, String... fields);

  /**
   * Get the type name for the model class.
   * @return The type name for the used model class
   */
  String getType();

  /**
   * Get the model class
   * @return Model class
   */
  Class<?> getModelClass();
  
  /**
   * Get the database instance associated with the controller
   * @return Database instance
   */
  GraphDatabaseService graph();
  
  void initialize();

  /**
   * Query builder instance to use.
   * @return New query builder instance
   */
  QueryBuilder queryBuilder();

  /**
   * Identifier used in queries.
   * @return The query identifier used for this repository
   */
  String queryIdentifier();

  /**
   * Delete a model
   * @param model Model to delete
   */
  void remove(T model);

  /**
   * Save a model
   * @param model model to save
   */
  void save(T model);

  /**
   * Save some entities.
   * @param entities Entities to save.
   */
  @SuppressWarnings("unchecked")
  void save(T... entities);

  /**
   * Save the model inside the passed SaveContext.
   * @param context Must contain the model to save
   */
  void save(SaveContext<T> context);

  /**
   * Get a search provider. Use its methods to filter and return the reusults.
   * @return Search provider
   */
  Search search();

  /**
   * Reference to the service the repository is assigned to.
   * @return Service instance
   */
  Service service();

  Persistence persistence();

  Relationships relationships();

  /**
   * Sort a list like you would in a database call.
   * @param list List of models to sort
   * @param orderBy Sorting conditions
   */
  void sort(List<T> list, List<SearchParameter.OrderBy> orderBy);
}
