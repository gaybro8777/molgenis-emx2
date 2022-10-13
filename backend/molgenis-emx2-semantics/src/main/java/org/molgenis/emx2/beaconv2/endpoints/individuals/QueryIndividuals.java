package org.molgenis.emx2.beaconv2.endpoints.individuals;

import static org.molgenis.emx2.beaconv2.common.QueryHelper.mapListToOntologyTerms;
import static org.molgenis.emx2.beaconv2.common.QueryHelper.mapToOntologyTerm;
import static org.molgenis.emx2.beaconv2.endpoints.individuals.IndividualsFields.*;

import graphql.ExecutionResult;
import graphql.GraphQL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.beaconv2.common.AgeAndAgeGroup;
import org.molgenis.emx2.graphql.GraphqlApiFactory;
import org.molgenis.emx2.utils.TypeUtils;

public class QueryIndividuals {

  /**
   * Construct GraphQL query on Beacon v2 individuals, with optional filters like "{sex:
   * {ontologyTermURI: {like: "http://purl.obolibrary.org/obo/GSSO_000124"}}}", "{ diseases: {
   * diseaseCode: {ontologyTermURI: {like: "Orphanet_1873"}}}}", etc. Beacon supports only AND
   * operator for filters.
   *
   * @param tables
   * @param filters
   * @return
   */
  public static List<IndividualsResultSets> queryIndividuals(
      List<Table> tables, String... filters) {
    List<IndividualsResultSets> resultSetsList = new ArrayList<>();

    StringBuffer concatFilters = new StringBuffer();
    for (String filter : filters) {
      concatFilters.append(filter + ",");
    }
    concatFilters.deleteCharAt(concatFilters.length() - 1);

    for (Table table : tables) {
      List<IndividualsResultSetsItem> individualsItemList = new ArrayList<>();

      GraphQL grapql = new GraphqlApiFactory().createGraphqlForSchema(table.getSchema());
      ExecutionResult executionResult =
          grapql.execute(
              "{Individuals"
                  + "(filter: { _and: [ "
                  + concatFilters
                  + " ] }  )"
                  + "{"
                  + "id,"
                  + "sex{name,codesystem,code},"
                  + AGE_AGEGROUP
                  + "{name,codesystem,code},"
                  + AGE_AGE_ISO8601DURATION
                  + ","
                  + "diseaseCausalGenes{name,codesystem,code},"
                  + "ethnicity{name,codesystem,code},"
                  + "geographicOrigin{name,codesystem,code},"
                  + "phenotypicFeatures{"
                  + "   featureType{name,codesystem,code},"
                  + "   modifiers{name,codesystem,code},"
                  + "   severity{name,codesystem,code}},"
                  + "diseases{"
                  + DISEASECODE
                  + "{name,codesystem,code},"
                  + AGEOFONSET_AGEGROUP
                  + "{name,codesystem,code},"
                  + AGEOFONSET_AGE_ISO8601DURATION
                  + ","
                  + AGEATDIAGNOSIS_AGEGROUP
                  + "{name,codesystem,code},"
                  + AGEATDIAGNOSIS_AGE_ISO8601DURATION
                  + ","
                  + FAMILYHISTORY
                  + ","
                  + SEVERITY
                  + "{name,codesystem,code},"
                  + STAGE
                  + "{name,codesystem,code}},"
                  + "measures{"
                  + "   assayCode{name,codesystem,code},"
                  + "   date,"
                  + "   measurementVariable,"
                  + "   measurementValue__value,"
                  + "   measurementValue__units{name,codesystem,code},"
                  + "   observationMoment__age__iso8601duration"
                  + "}}}");

      Map<String, Object> result = executionResult.toSpecification();

      List<Map<String, Object>> individualsListFromJSON =
          (List<Map<String, Object>>)
              ((HashMap<String, Object>) result.get("data")).get("Individuals");

      if (individualsListFromJSON != null) {
        for (Map map : individualsListFromJSON) {
          IndividualsResultSetsItem individualsItem = new IndividualsResultSetsItem();
          individualsItem.setId(TypeUtils.toString(map.get("id")));
          individualsItem.setSex(mapToOntologyTerm(map.get("sex")));
          individualsItem.setAge(
              new AgeAndAgeGroup(
                  mapToOntologyTerm(map.get(AGE_AGEGROUP)),
                  TypeUtils.toString(map.get(AGE_AGE_ISO8601DURATION))));
          individualsItem.setDiseaseCausalGenes(
              mapListToOntologyTerms(map.get("diseaseCausalGenes")));
          individualsItem.setEthnicity(mapToOntologyTerm((map.get("ethnicity"))));
          individualsItem.setGeographicOrigin(mapToOntologyTerm(map.get("geographicOrigin")));
          individualsItem.setPhenotypicFeatures(
              PhenotypicFeatures.get(map.get("phenotypicFeatures")));
          individualsItem.setDiseases(Diseases.get(map.get("diseases")));
          individualsItem.setMeasures(Measures.get(map.get("measures")));
          individualsItemList.add(individualsItem);
        }
      }

      if (individualsItemList.size() > 0) {
        IndividualsResultSets individualsResultSets =
            new IndividualsResultSets(
                table.getSchema().getName(),
                individualsItemList.size(),
                individualsItemList.toArray(
                    new IndividualsResultSetsItem[individualsItemList.size()]));
        resultSetsList.add(individualsResultSets);
      }
    }
    return resultSetsList;
  }
}