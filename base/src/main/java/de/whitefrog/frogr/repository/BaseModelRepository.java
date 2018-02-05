package de.whitefrog.frogr.repository;

import de.whitefrog.frogr.exception.FrogrException;
import de.whitefrog.frogr.exception.PersistException;
import de.whitefrog.frogr.exception.TypeMismatchException;
import de.whitefrog.frogr.model.FieldList;
import de.whitefrog.frogr.model.Model;
import de.whitefrog.frogr.model.SaveContext;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.PropertyContainer;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Base repository for models.
 * Provides basic functionality like model creation and persistence.
 */
public abstract class BaseModelRepository<T extends Model> extends BaseRepository<T> implements ModelRepository<T> {
  private final Label label;
  private Set<Label> labels;

  public BaseModelRepository() {
    super();
    this.label = Label.label(getType());
  }
  public BaseModelRepository(String modelName) {
    super(modelName);
    this.label = Label.label(modelName);
  }

  @Override
  @SuppressWarnings("unchecked")
  public T createModel() {
    try {
      return (T) getModelClass().newInstance();
    } catch(ReflectiveOperationException e) {
      throw new FrogrException(e.getMessage(), e);
    }
  }

  @Override
  public T createModel(PropertyContainer node, FieldList fields) {
    if(node instanceof Node && !checkType((Node) node)) {
      throw new TypeMismatchException((Node) node, label());
    }

    return service().persistence().get(node, fields);
  }

  private boolean checkType(Node node) {
    return node.hasLabel(label());
  }

  public T find(long id, FieldList fields) {
    try {
      T model = createModel(graph().getNodeById(id), fields);
      if(CollectionUtils.isNotEmpty(fields)) fetch(model, fields);
      return model;
    } catch(IllegalStateException e) {
      logger().warn(e.getMessage(), e);
      return null;
    } catch(NotFoundException e) {
      return null;
    }
  }

  @Override
  public Node getNode(Model model) {
    Validate.notNull(model, "The model is null");
    try {
      return service().graph().getNodeById(model.getId());
    } catch(NotFoundException e) {
      throw new IllegalStateException(e.getMessage(), e);
    }
  }

  @Override
  public Label label() {
    return label;
  }

  @Override
  public Set<Label> labels() {
    if(labels == null) {
      labels = getModelInterfaces(getModelClass()).stream()
        .map(Label::label)
        .collect(Collectors.toSet());
    }
    return labels;
  }

  @Override
  public void remove(T model) throws PersistException {
    service().persistence().delete(model);
    logger().info("{} deleted", model);
  }

  @Override
  public void save(SaveContext<T> context) throws PersistException {
    validateModel(context);
    boolean create = !context.model().getPersisted();
    service().persistence().save(this, context);
    logger().info("{} {}", context.model(), create? "created": "updated");
  }
}
