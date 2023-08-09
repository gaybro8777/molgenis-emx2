# ///////////////////////////////////////////////////////////////////////////////
# FILE: dev.py
# AUTHOR: David Ruvolo, Ype Zijlstra
# CREATED: 2023-05-22
# MODIFIED: 2023-08-07
# PURPOSE: development script for initial testing of the py-client
# STATUS: ongoing
# PACKAGES: pandas
# COMMENTS: Designed to interact with the schema "pet store".
#           Create a file called '.env'
# ///////////////////////////////////////////////////////////////////////////////
import os

import pandas as pd
from dotenv import load_dotenv

from tools.pyclient.src.molgenis_emx2_pyclient import Client
from tools.pyclient.src.molgenis_emx2_pyclient.exceptions import NoSuchSchemaException


def main():
    # Load the login details into the environment
    load_dotenv()
    username = os.environ.get('MG_USERNAME')
    password = os.environ.get('MG_PASSWORD')

    # Connect to the server and sign in
    client = Client('https://emx2.dev.molgenis.org/')
    client.sign_in(username, password)

    # Check sign in status
    client.status()

    # Get data
    try:
        data = client.get(schema='', table='')  # run without specifying target
        print(data)
    except NoSuchSchemaException as e:
        print(e)
    try:
        data = client.get(schema='pet store', table='')  # run without specifying table
        print(data)
    except NoSuchSchemaException as e:
        print(e)
    try:
        data = client.get(schema='pet store', table='Pet')  # get Pets
        print(data)
    except NoSuchSchemaException as e:
        print(e)
    try:
        data = client.get(schema='pet store', table='Pet', as_df=True)  # get Pets
        print(data)
    except NoSuchSchemaException as e:
        print(e)

    # ///////////////////////////////////////////////////////////////////////////////

    # ~ 1 ~
    # Check Import Methods

    # ~ 1a ~
    # Check import via the `data` parameters
    # Add new record to the pet store with new tags

    new_tags = [
        {'name': 'brown', 'parent': 'colors'},
        {'name': 'canis', 'parent': 'species'},
    ]

    new_pets = [{
        'name': 'Woofie',
        'category': 'dog',
        'status': 'available',
        'weight': 6.8,
        'tags': 'brown,canis'
    }]

    # Import new data
    client.add(schema='pet store', table='Tag', data=new_tags)
    client.add(schema='pet store', table='Pet', data=new_pets)

    # Retrieve records
    data = client.get(schema='pet store', table='Pet')
    print(data)

    # Drop records
    tags_to_remove = [{'name': row['name']} for row in new_tags if row['name'] == 'canis']
    client.delete(schema='pet store', table='Pet', data=new_pets)
    client.delete(schema='pet store', table='Tag', data=tags_to_remove)

    # ///////////////////////////////////////

    # ~ 1b ~
    # Check import via the `file` parameter

    # Save datasets
    pd.DataFrame(new_tags).to_csv('dev/demodata/Tag.csv', index=False)
    pd.DataFrame(new_pets).to_csv('dev/demodata/Pet.csv', index=False)

    # Import files
    client.add(schema='pet store', table='Tag', file='dev/demodata/Tag.csv')
    client.add(schema='pet store', table='Pet', file='dev/demodata/Pet.csv')

    client.delete(schema='pet store', table='Pet', file='dev/demodata/Pet.csv')
    client.delete(schema='pet store', table='Tag', file='dev/demodata/Tag.csv')

    # Sign out
    client.sign_out()


if __name__ == '__main__':
    main()
