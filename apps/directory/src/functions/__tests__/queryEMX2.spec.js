import QueryEMX2 from "../queryEMX2";

describe('QueryEMX2 Interface', () => {
  it('can create a simple query on the biobanks table', () => {
    const query = new QueryEMX2('graphql').table('Biobanks').select('id').getQuery()

    expect(query).toStrictEqual(`{
Biobanks {
    id
  }
}`
    )
  })

  it('can query a table to retrieve id and name by default', () => {
    const query = new QueryEMX2('graphql').table('Biobanks').getQuery()

    expect(query).toStrictEqual(`{
Biobanks {
    id,
    name
  }
}`
    )
  })

  it('can limit the search results', () => {
    const query = new QueryEMX2('graphql')
      .table('Biobanks')
      .select('id')
      .limit("biobanks", 100)
      .getQuery()

    expect(query).toStrictEqual(`{
Biobanks(limit: 100) {
    id
  }
}`
    )
  })

  it('can apply search to the table', () => {
    const query = new QueryEMX2('graphql')
      .table('Biobanks')
      .select('id')
      .find('something')
      .getQuery()

    expect(query).toStrictEqual(`{
Biobanks(search: "something") {
    id
  }
}`
    )
  })


  it('can create a query when selecting multiple columns on the biobanks table', () => {
    const query = new QueryEMX2('graphql').table('Biobanks').select(['id', 'name']).getQuery()

    expect(query).toStrictEqual(`{
Biobanks {
    id,
    name
  }
}`)
  })

  it('can create a query with a filter', () => {
    const query = new QueryEMX2('graphql')
      .table('Biobanks')
      .select(['id', 'name'])
      .where('name').like('UMC')
      .getQuery()

    expect(query).toStrictEqual(`{
Biobanks(filter: { name: { like: "UMC"} }) {
    id,
    name
  }
}`
    )
  })

  it('can create a query with and by default when multiple filters are applied', () => {
    const query = new QueryEMX2('graphql')
      .table('Biobanks')
      .select(['id', 'name'])
      .where('name').like('Dresden')
      .where('country', 'name').equals('DE')
      .getQuery()

    expect(query).toStrictEqual(`{
Biobanks(filter: { name: { like: "Dresden"}, country: { name: { equals: "DE"} } }) {
    id,
    name
  }
}`)
  })

  it('can create a filter query on a nested property, like collections', () => {
    const query = new QueryEMX2('graphql')
      .table('Biobanks')
      .select(['id', 'name'])
      .where('collections', 'id').like('Lifelines')
      .where('collections', 'name').like('Lifelines')
      .getQuery()

    expect(query).toStrictEqual(`{
Biobanks(filter: { collections: { id: { like: "Lifelines"}, name: { like: "Lifelines"} } }) {
    id,
    name
  }
}`)
  })

  it('can create a query with a where and a limit', () => {
    const query = new QueryEMX2('graphql')
      .table('Biobanks')
      .select(['id', 'name'])
      .where('name').like('UMC')
      .limit("biobanks", 100)
      .getQuery()

    expect(query).toStrictEqual(`{
Biobanks(limit: 100, filter: { name: { like: "UMC"} }) {
    id,
    name
  }
}`
    )
  })

  it('can create a query with a where, a limit and orderby', () => {
    const query = new QueryEMX2('graphql')
      .table('Biobanks')
      .select(['id', 'name'])
      .where('name').like('UMC')
      .limit("biobanks", 100)
      .orderBy("biobanks", "name", "asc")
      .getQuery()

    expect(query).toStrictEqual(`{
Biobanks(limit: 100, orderby: { name: ASC }, filter: { name: { like: "UMC"} }) {
    id,
    name
  }
}`
    )
  })

  it('can create and query an array as value', () => {
    const query = new QueryEMX2('graphql')
      .table('Biobanks')
      .select(['id', 'name'])
      .where('name').like(['UMC', 'Dresden'])
      .getQuery()

    expect(query).toStrictEqual(`{
Biobanks(filter: { name: { like: ["UMC","Dresden"]} }) {
    id,
    name
  }
}`)
  })

  it('can create a query with an or clause', () => {
    const query = new QueryEMX2('graphql')
      .table('Biobanks')
      .select(['id', 'name'])
      .where('name').like("Dresden")
      .or('country', "name").like('DE')
      .getQuery()

    expect(query).toStrictEqual(`{
Biobanks(filter: { name: { like: "Dresden"}, _or: { country: { name: { like: "DE"} } } }) {
    id,
    name
  }
}`
    )
  })

  it('can produce a query with nested properties when adding an object to the selection array', () => {
    const query = new QueryEMX2('graphql')
      .table('Biobanks')
      .select(['id', 'name', { collections: ['id', 'name'] }])
      .getQuery()

    expect(query).toStrictEqual(`{
Biobanks {
    id,
    name,
    collections {
        id,
        name
    }
  }
}`
    )
  })

  it('can produce a query with nested properties and nested filters when you have an object to the selection array and a filter clause', () => {
    const query = new QueryEMX2('graphql')
      .table('Biobanks')
      .select(['id', 'name', { collections: ['id', 'name'] }])
      .filter('Collections', 'Name').like('cardiovascular')
      .getQuery()

    expect(query).toStrictEqual(`{
Biobanks {
    id,
    name,
    collections(filter: { name: { like: "cardiovascular"} }) {
        id,
        name
    }
  }
}`
    )
  })

  it('can nest multiple tables with the objects in array structure', () => {
    /** just an example */

    const basic = ['id', 'name']
    const selection = [
      ...basic,
      {
        LayerA: [
          ...basic,
          {
            layerB: [
              ...basic,
              {
                layerC: [
                  ...basic
                ]
              }]
          }]
      }]


    const query = new QueryEMX2('graphql')
      .table('NestedExample')
      .select(selection)
      .getQuery()

    expect(query).toStrictEqual(`{
NestedExample {
    id,
    name,
    layerA {
        id,
        name,
        layerB {
            id,
            name,
            layerC {
                id,
                name
            }
        }
    }
  }
}`
    )
  })

  it('can filter on any nested property', () => {
    /** just an example */

    const basic = ['id', 'name']
    const selection = [
      ...basic,
      {
        LayerA: [
          ...basic,
          {
            layerB: [
              ...basic,
              {
                layerC: [
                  ...basic
                ]
              }]
          }]
      }]


    const query = new QueryEMX2('graphql')
      .table('NestedExample')
      .select(selection)
      .filter('layerC', 'name').like('nameOfC')
      .getQuery()

    expect(query).toStrictEqual(`{
NestedExample {
    id,
    name,
    layerA {
        id,
        name,
        layerB {
            id,
            name,
            layerC(filter: { name: { like: "nameOfC"} }) {
                id,
                name
            }
        }
    }
  }
}`
    )
  })
})
