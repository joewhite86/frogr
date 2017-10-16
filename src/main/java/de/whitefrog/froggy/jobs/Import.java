package de.whitefrog.froggy.jobs;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.whitefrog.froggy.Service;
import de.whitefrog.froggy.model.relationship.BaseRelationship;
import org.neo4j.graphdb.Transaction;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Import {
  public static void main(String[] args) throws Exception {
    try(Service service = new Service()) {
      service.connect();
      ObjectMapper mapper = new ObjectMapper();

      try(Transaction tx = service.beginTx()) {
        File file = new File(Export.path + "/models.json");
//        ExecutionResultIterator<Model> iterator = new ExecutionResultIterator<>(service,
//          service.graph().execute("match (e) return e"), "e");
//        List<Model> models = Iterators.asList(Iterables.get(iterator));
//        mapper.writerWithDefaultPrettyPrinter().writeValue(file, models);

        file = new File(Export.path + "/relationships.json");
        List<BaseRelationship> relationships = new ArrayList<>();
//        for(Base model : models) {
//          Node node = Persistence.getRelationship(model);
//          for(Relationship relationship : node.getRelationships(Direction.OUTGOING)) {
//            relationships.add(new DefaultRelationship(service).getRelationship(
//              model.clone(Arrays.asList(Base.IdProperty, Base.Uuid)), relationship,
//              Arrays.asList(Base.IdProperty, Base.Uuid)));
//          }
//        }
        mapper.writerWithDefaultPrettyPrinter().writeValue(file, relationships);
      }
    }
  }
}
