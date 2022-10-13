import gql from "graphql-tag";
export default gql`
  query CollectionEvents($pid: String, $name: String) {
    CollectionEvents(
      filter: {
        resource: { pid: { equals: [$pid] } }
        name: { equals: [$name] }
      }
    ) {
      resource {
        name
      }
      name
      description
      startYear {
        name
      }
      startMonth {
        name
      }
      endYear {
        name
      }
      endMonth {
        name
      }
      numberOfParticipants
      ageGroups {
        name
      }
      dataCategories {
        name
      }
      sampleCategories {
        name
      }
      areasOfInformation {
        name
      }
      subcohorts {
        name
      }
      coreVariables {
        name
      }
    }
  }
`;