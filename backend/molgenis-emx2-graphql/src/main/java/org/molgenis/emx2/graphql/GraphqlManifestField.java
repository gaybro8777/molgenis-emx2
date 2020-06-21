package org.molgenis.emx2.graphql;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import org.molgenis.emx2.Version;

import java.util.HashMap;
import java.util.Map;

public class GraphqlManifestField {

  public static final String IMPLEMENTATION_VERSION = "ImplementationVersion";
  public static final String SPECIFICATION_VERSION = "SpecificationVersion";

  public static GraphQLFieldDefinition.Builder queryVersionField() {
    return GraphQLFieldDefinition.newFieldDefinition()
        .name("_manifest")
        .dataFetcher(
            dataFetchingEnvironment -> {
              Map<String, String> result = new HashMap<>();
              result.put(IMPLEMENTATION_VERSION, Version.getImplementationVersion());
              result.put(SPECIFICATION_VERSION, Version.getSpecificationVersion());
              return result;
            })
        .type(
            GraphQLObjectType.newObject()
                .name("Manifest")
                .field(
                    GraphQLFieldDefinition.newFieldDefinition()
                        .name(IMPLEMENTATION_VERSION)
                        .type(Scalars.GraphQLString)
                        .build())
                .field(
                    GraphQLFieldDefinition.newFieldDefinition()
                        .name(SPECIFICATION_VERSION)
                        .type(Scalars.GraphQLString)
                        .build())
                .build());
  }
}
