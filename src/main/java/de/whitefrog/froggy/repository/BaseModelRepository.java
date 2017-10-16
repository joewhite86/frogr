package de.whitefrog.froggy.repository;

import de.whitefrog.froggy.Service;
import de.whitefrog.froggy.exception.NeobaseRuntimeException;
import de.whitefrog.froggy.exception.PersistException;
import de.whitefrog.froggy.exception.TypeMismatchException;
import de.whitefrog.froggy.model.Model;
import de.whitefrog.froggy.model.SaveContext;
import de.whitefrog.froggy.model.rest.FieldList;
import de.whitefrog.froggy.persistence.Persistence;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.neo4j.graphdb.*;

import java.util.Set;
import java.util.stream.Collectors;

public abstract class BaseModelRepository<T extends Model> extends BaseRepository<T> implements ModelRepository<T> {
  private final Label label;
  private Set<Label> labels;

  public BaseModelRepository(Service service) {
    super(service);
    this.label = Label.label(getType());
    if(getModelClass() != null) {
      this.labels = getModelInterfaces(getModelClass()).stream()
        .map(Label::label)
        .collect(Collectors.toSet());
    } else {
      logger().warn("no model class found for {}", getClass());
    }
  }
  public BaseModelRepository(Service service, String modelName) {
    super(service, modelName);
    this.label = Label.label(modelName);
    this.labels = getModelInterfaces(getModelClass()).stream()
      .map(Label::label)
      .collect(Collectors.toSet());
  }

  @Override
  public T createModel() {
    try {
      return (T) getModelClass().newInstance();
    } catch(ReflectiveOperationException e) {
      throw new NeobaseRuntimeException(e.getMessage(), e);
    }
  }

  @Override
  public T createModel(PropertyContainer node, FieldList fields) {
    if(node instanceof Node && !checkType((Node) node)) {
      throw new TypeMismatchException((Node) node, label());
    }

    return Persistence.get(node, fields);
  }

  boolean checkType(Node node) {
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
    Validate.notNull(model.getId(), "ID can not be null.");
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
    return labels;
  }

  @Override
  public void remove(T model) throws PersistException {
    Validate.notNull(model.getId(), "'id' is required");
    Persistence.delete(this, model);
    logger().info("{} deleted", model);
  }

  @Override
  public void save(SaveContext<T> context) throws PersistException {
    validateModel(context);
    boolean create = !context.model().getPersisted();
    Persistence.save(this, context);
    logger().info("{} {}", context.model(), create? "created": "updated");
  }
}
