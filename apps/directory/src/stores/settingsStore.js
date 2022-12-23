import { ref } from 'vue'
import { defineStore } from 'pinia'
import { i18n } from '../i18n/i18n'
import { initialFilterFacets } from '../filter-config/initialFilterFacets'
import initialCollectionColumns from '../property-config/initialCollectionColumns'
import initialBiobankColumns from '../property-config/initialBiobankColumns'
import initialBiobankCardColumns from '../property-config/initialBiobankCardsColumns'
import QueryEMX2 from './queryEMX2'
/**
 * Settings store is where all the configuration of the application is handled.
 * This means that user config from the database is merged with the defaults here.
 */
export const useSettingsStore = defineStore('settingsStore', () => {
  let session = ref({})
  const currentPage = ref(1)

  let config = ref({
    graphqlEndpoint: 'graphql',
    negotiatorType: 'eric-negotiator',
    biobankColumns: initialBiobankColumns,
    biobankCardColumns: initialBiobankCardColumns,
    collectionColumns: initialCollectionColumns,
    filterFacets: initialFilterFacets,
    filterMenuInitiallyFolded: false,
    biobankCardShowCollections: true,
    menuHeight: 50,
    pageSize: 12,
    i18n
  })

  async function initializeConfig () {
    const settingsResult = await new QueryEMX2(config.value.graphqlEndpoint)
      .table('_settings')
      .select(['key', 'value'])
      .execute();

      return settingsResult
  }

  async function getCurrentSession () {
    if (!Object.keys(session.value).length) {
      const sessionResult = await new QueryEMX2(config.value.graphqlEndpoint)
        .table('_session')
        .select(['email', 'roles'])
        .execute();
      session.value = { ...sessionResult._session }
    }
    return session.value
  }

  // todo add config from database
  // todo add config management functions

  return { config, currentPage, getCurrentSession, initializeConfig }
})
