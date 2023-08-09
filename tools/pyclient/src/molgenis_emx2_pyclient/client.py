import csv
import io
import logging

import pandas as pd
import requests

from . import graphql_queries as queries
from . import utils as utils
from .exceptions import NoSuchSchemaException, ServiceUnavailableError, SigninError, ServerNotFoundError, \
    PyclientException, NoSuchTableException

log = logging.getLogger(__name__)


class Client:
    """
    Use the Client object to log in to a Molgenis server and perform operations on the server.
    """
    def __init__(self, url: str, username: str = None, password: str = None) -> None:
        """
        A Client class instances is created with a server url, username and password.
        The object starts a network session and logs in to the server using the login credentials.
        """
        self.url = utils.parse_url(url)
        self.api_graphql = self.url + "/apps/central/graphql"

        self.username = username
        self.signin_status = 'unknown'

        self.session = requests.Session()

        # Sign in when user credentials are supplied
        if username is not None and password is not None:
            self.sign_in(username, password)

    def __str__(self):
        return self.url

    def sign_in(self, username: str, password: str):
        """Signs in to Molgenis and retrieves session cookie.

        :param username: the username or email address for an account on this server
        :type username: str
        :param password: the password corresponding to this username.
        :type username: str
        """
        query = queries.sign_in()
        variables = {'email': username, 'password': password}

        response = self.session.post(
            url=f'{self.url}/apps/central/graphql',
            json={'query': query, 'variables': variables}
        )

        if response.status_code == 404:
            raise ServerNotFoundError(f"Server '{self.url}' could not be found. "
                                      f"Ensure the spelling of the url is correct.")
        if response.status_code == 503:
            raise ServiceUnavailableError(f"Server '{self.url}' not available. "
                                          f"Try again later.")
        if response.status_code != 200:
            raise PyclientException(f"Server '{self.url}' could not be reached due to a connection problem."
                                    f"\nStatus code: {response.status_code}. Reason: '{response.reason}'.")

        response_json: dict = response.json().get('data', {}).get('signin', {})
        
        if response_json.get('status') == 'SUCCESS':
            self.signin_status = 'success'
            message = f"Success: Signed in to {self.url} as {username}."
            log.info(message)
            print(message)
        elif response_json.get('status') == 'FAILED':
            self.signin_status = 'failed'
            message = f"Error: Unable to sign in to {self.url} as {username}." \
                      f"\n{response_json.get('message')}"
            log.error(message)
            raise SigninError(message)
        else:
            self.signin_status = 'failed'
            message = f"Error: Unable to sign in to {self.url} as {username}." \
                      f"\n{response_json.get('message')}"
            log.error(message)
            raise SigninError(message)

        self.username = username

    def sign_out(self):
        """Signs the client out of the EMX2 server."""
        response = self.session.post(
            url=self.api_graphql,
            json={'query': queries.sign_out()}
        )        
        
        status = response.json().get('data', {}).get('signout', {}).get('status')
        message = response.json().get('data', {}).get('signout', {}).get('message')
        if status == 'SUCCESS':
            print(f"Signed out of {self.url}")
        else:
            print(f"Unable to sign out of {self.url}.")
            print(message)
            
    @property
    def status(self):
        """View client information"""
        schemas = '\n\t'.join(self.schemas)
        message = (
          f"Host: {self.url}\n"
          f"User: {self.username}\n"
          f"Status: {'Signed in' if self.signin_status == 'success' else 'Logged out'}\n"
          f"Schemas: \n\t{schemas}\n"
          f"Version: {self.version}\n"
        )
        return message

    @property
    def schemas(self):
        """List the databases present on the server."""
        query = queries.list_schemas()

        response = self.session.post(
            url=f'{self.url}/apps/central/graphql',
            json={'query': query}
        )

        response_json: dict = response.json()

        databases = response_json['data']['_schemas']
        database_names = [db['name'] for db in databases]

        return database_names
    
    @staticmethod
    def _prep_data_or_file(file_path: str = None, data: list = None) -> str:
        """Prepares the data from memory or loaded from disk for addition or deletion action.

        :param file_path: path to the file to be prepared
        :type file_path: str
        :param data: data to be prepared
        :type data: list

        :returns: prepared data in dataframe format
        :rtype: pd.DataFrame
        """
        if file_path is None and data is None:
            print("No data to import. Specify a file location or a dataset.")
        
        if file_path is not None:
            return utils.read_file(file_path=file_path)
          
        if data is not None:
            return pd.DataFrame(data).to_csv(index=False, quoting=csv.QUOTE_NONNUMERIC, encoding='UTF-8')
    
    def add(self, schema: str, table: str, file: str = None, data: list = None):
        """Imports or updates records in a table of a named schema.
        
        :param schema: name of a schema
        :type schema: str
        :param table: the name of the table
        :type table: str
        :param file: location of the file containing records to import or update
        :type file: str
        :param data: a dataset containing records to import or update (list of dictionaries)
        :type data: list
        
        :returns: status message or response
        :rtype: str
        """
        import_data = self._prep_data_or_file(file_path=file, data=data)

        if schema not in self.schemas:
            raise NoSuchSchemaException(f"Schema '{schema}' not found on server.")

        if not self._table_in_schema(table, schema):
            raise NoSuchTableException(f"Table '{table}' not found in schema '{schema}'.")

        response = self.session.post(
            url=f"{self.url}/{schema}/api/csv/{table}",
            headers={'Content-Type': 'text/csv'},
            data=import_data
        )
        
        if response.status_code == 200:
            log.info(f"Imported data into {schema}::{table}.")
        else:
            errors = '\n'.join([err['message'] for err in response.json().get('errors')])
            log.error(f"Failed to import data into {schema}::{table}\n{errors}.")

    def delete(self, schema: str, table: str, file: str = None, data: list = None):
        """Deletes records from table.
        
        :param schema: name of a schema
        :type schema: str
        :param table: the name of the table
        :type table: str
        :param file: location of the file containing records to import or update
        :type file: str
        :param data: a dataset containing records to import or update (list of dictionaries)
        :type data: list

        :returns: status message or response
        :rtype: str
        """
        import_data = self._prep_data_or_file(file_path=file, data=data)

        if schema not in self.schemas:
            raise NoSuchSchemaException(f"Schema '{schema}' not found on server.")

        if not self._table_in_schema(table, schema):
            raise NoSuchTableException(f"Table '{table}' not found in schema '{schema}'.")

        response = self.session.delete(
            url=f"{self.url}/{schema}/api/csv/{table}",
            headers={'Content-Type': 'text/csv'},
            data=import_data
        )
        
        if response.status_code == 200:
            log.info(f"Deleted data from {schema}::{table}.")
        else:
            errors = '\n'.join([err['message'] for err in response.json().get('errors')])
            log.error(f"Failed to delete data from {schema}::{table}\n{errors}.")
    
    def get(self, schema: str, table: str, as_df: bool = False) -> list | pd.DataFrame:
        """Retrieves data from a schema and returns as a list of dictionaries or as
        a pandas DataFrame (as pandas is used to parse the response).
        
        :param schema: name of a schema
        :type schema: str
        :param table: the name of the table
        :type table: str
        :param as_df: if True, the response will be returned as a
                      pandas DataFrame. Otherwise, a recordset will be returned.
        :type as_df: bool
        
        :returns: list of dictionaries, status message or data frame
        :rtype: list | pd.DataFrame
        """
        if schema not in self.schemas:
            raise NoSuchSchemaException(f"Schema '{schema}' not found on server.")

        if not self._table_in_schema(table, schema):
            raise NoSuchTableException(f"Table '{table}' not found in schema '{schema}'.")

        response = self.session.get(url=f"{self.url}/{schema}/api/csv/{table}")

        if response.status_code != 200:
            message = f"Failed to retrieve data from '{schema}::{table}'." \
                      f"\nStatus code: {response.status_code}."
            log.error(message)
            raise PyclientException(message)

        response_data = pd.read_csv(io.BytesIO(response.content))

        if not as_df:
            return response_data.to_dict('records')
        return response_data

    def _table_in_schema(self, table: str, schema: str) -> bool:
        """Checks whether the requested table is present in the schema.

        :param table: the name of the table
        :type table: str
        :param schema: the name of the schema
        :type schema: str
        :returns: boolean indicating whether table is present
        :rtype: bool
        """
        response = self.session.post(
            url=f"{self.url}/{schema}/graphql",
            json={'query': queries.list_tables()}
        )
        schema_tables = [tab['name'] for tab in
                         response.json().get('data').get('_schema').get('tables')]
        return table in schema_tables

    @property
    def version(self):
        query = queries.version_number()
        response = self.session.post(
            url=self.api_graphql,
            json={'query': query}
        )
        return response.json().get('data').get('_manifest').get('SpecificationVersion')
