package org.molgenis.emx2.sql;

import org.jooq.Record;
import org.jooq.exception.DataAccessException;
import org.molgenis.emx2.*;
import org.molgenis.emx2.utils.MolgenisException;

import java.util.*;

import static org.jooq.impl.DSL.*;

public class SqlSchema implements Schema {
  private SqlDatabase db;
  private SchemaMetadata metadata;

  public SqlSchema(SqlDatabase db, SchemaMetadata metadata) {
    this.db = db;
    this.metadata = metadata;
  }

  @Override
  public SqlTable getTable(String name) throws MolgenisException {
    return new SqlTable(db, getMetadata().getTableMetadata(name));
  }

  @Override
  public List<Member> getMembers() throws MolgenisException {
    List<Member> members = new ArrayList<>();

    // retrieve all role members
    String roleFilter = getRolePrefix();
    String userFilter = SqlTable.MG_USER_PREFIX;
    List<Record> result =
        db.getJooq()
            .fetch(
                "select m.rolname as member, r.rolname as role"
                    + " from pg_catalog.pg_auth_members am "
                    + " join pg_catalog.pg_roles m on (m.oid = am.member)"
                    + "join pg_catalog.pg_roles r on (r.oid = am.roleid)"
                    + "where r.rolname ILIKE {0} and m.rolname ILIKE {1}",
                roleFilter + "%", userFilter + "%");
    for (Record r : result) {
      String memberName = r.getValue("member", String.class).substring(userFilter.length());
      String roleName = r.getValue("role", String.class).substring(roleFilter.length());
      members.add(new Member(memberName, roleName));
    }

    return members;
  }

  @Override
  public void addMembers(List<Member> members) throws MolgenisException {
    try {
      transaction(
          database -> {
            List<String> currentRoles = getRoles();
            List<Member> currentMembers = getMembers();

            for (Member m : members) {
              if (!currentRoles.contains(m.getRole()))
                throw new MolgenisException(
                    "add_members_failed",
                    "Add member(s) failed",
                    "Role '"
                        + m.getRole()
                        + " doesn't exist in schema '"
                        + getMetadata().getName()
                        + "'. Existing roles are: "
                        + currentRoles);

              String username = SqlTable.MG_USER_PREFIX + m.getUser();
              String roleprefix = getRolePrefix();
              String rolename =
                  SqlTable.MG_ROLE_PREFIX + getMetadata().getName().toUpperCase() + m.getRole();

              // createTableIfNotExists user if not exists
              db.addUser(m.getUser());

              // give god powers.
              if (DefaultRoles.OWNER.toString().equals(m.getRole())) {
                db.getJooq().execute("ALTER ROLE {0} CREATEROLE", name(username));
              }

              // revoke other roles if exists
              for (Member old : currentMembers) {
                if (old.getUser().equals(m.getUser())) {
                  db.getJooq()
                      .execute(
                          "REVOKE {0} FROM {1}", name(roleprefix + old.getRole()), name(username));
                }
              }

              // grant role
              db.getJooq().execute("GRANT {0} TO {1}", name(rolename), name(username));
            }
          });
    } catch (DataAccessException dae) {
      throw new SqlMolgenisException("add_members_failed", "Add member failed", dae);
    }
  }

  @Override
  public void addMembers(Member... members) throws MolgenisException {
    this.addMembers(Arrays.asList(members));
  }

  @Override
  public void addMember(String user, String role) throws MolgenisException {
    this.addMembers(new Member(user, role));
  }

  @Override
  public void removeMember(String user) throws MolgenisException {
    removeMembers(new Member(user, null));
  }

  @Override
  public void removeMembers(Member... members) throws MolgenisException {
    removeMembers(Arrays.asList(members));
  }

  @Override
  public void removeMembers(List<Member> members) throws MolgenisException {
    List<String> usernames = new ArrayList<>();
    for (Member m : members) usernames.add(m.getUser());

    String userprefix = SqlTable.MG_USER_PREFIX;
    String roleprefix = getRolePrefix();

    try {
      transaction(
          database -> {
            for (Member m : getMembers()) {
              if (usernames.contains(m.getUser())) {
                db.getJooq()
                    .execute(
                        "REVOKE {0} FROM {1}",
                        name(roleprefix + m.getRole()), name(userprefix + m.getUser()));
              }
            }
          });
    } catch (DataAccessException dae) {
      throw new SqlMolgenisException("remove_member_failed", "Remove of member failed", dae);
    }
  }

  @Override
  public List<String> getRoles() {
    List<String> result = new ArrayList<>();
    for (Record r :
        db.getJooq()
            .fetch(
                "select rolname from pg_catalog.pg_roles where rolname LIKE {0}",
                getRolePrefix() + "%")) {
      result.add(r.getValue("rolname", String.class).substring(getRolePrefix().length()));
    }
    return result;
  }

  @Override
  public String getRoleForUser(String user) throws MolgenisException {
    user = user.trim();
    for (Member m : getMembers()) {
      if (m.getUser().equals(user)) return m.getRole();
    }
    return null;
  }

  @Override
  public SqlTable createTableIfNotExists(String name) throws MolgenisException {
    try {
      return getTable(name);
    } catch (Exception e) {
      getMetadata().createTableIfNotExists(name);
      return getTable(name);
    }
  }

  @Override
  public Table createTableIfNotExists(TableMetadata metadata) throws MolgenisException {
    transaction(
        database -> {
          TableMetadata table = this.createTableIfNotExists(metadata.getTableName()).getMetadata();
          for (Column c : metadata.getColumns()) {
            table.addColumn(c);
          }
          if (metadata.getPrimaryKey().length > 0) table.setPrimaryKey(metadata.getPrimaryKey());
          for (String[] unique : metadata.getUniques()) {
            table.addUnique(unique);
          }
        });
    return getTable(metadata.getTableName());
  }

  @Override
  public SchemaMetadata getMetadata() {
    return metadata;
  }

  @Override
  public Collection<String> getTableNames() throws MolgenisException {
    return getMetadata().getTableNames();
  }

  private String getRolePrefix() {
    return SqlTable.MG_ROLE_PREFIX + getMetadata().getName().toUpperCase();
  }

  @Override
  public Query query(String tableName) throws MolgenisException {
    return getTable(tableName).query();
  }

  @Override
  public void transaction(Transaction transaction) throws MolgenisException {
    db.transaction(transaction);
  }

  @Override
  public void transaction(String role, Transaction transaction) throws MolgenisException {
    db.transaction(role, transaction);
  }

  @Override
  public void merge(SchemaMetadata from) throws MolgenisException {
    transaction(
        database -> {
          List<TableMetadata> tableList = new ArrayList<>();
          for (String tableName : from.getTableNames()) {
            tableList.add(from.getTableMetadata(tableName));
          }

          // todo circular dependencies
          Collections.sort(
              tableList,
              (a, b) -> {
                String aName = a.getTableName();
                String bName = b.getTableName();
                for (Column c : a.getColumns()) {
                  if (bName.equals(c.getRefTableName())) return 1;
                }
                for (Column c : b.getColumns()) {
                  if (aName.equals(c.getRefTableName())) return -1;
                }
                return 0;
              });

          for (TableMetadata table : tableList) {
            this.createTableIfNotExists(table);
          }
        });
  }
}
