# WildFly Deploy Guide

## Target
- WildFly 36+ (Jakarta EE 10)
- Artifact type: `war`

## Build

```powershell
.\gradlew clean bootWar
```

WAR file is generated in `build/libs/`.

## WildFly datasource

Configure datasource in WildFly and bind it to:

- `java:jboss/datasources/BlpsDS` (default)

If another JNDI name is used, set:

- `DB_JNDI_NAME`

and run Spring profile:

- `wildfly`

## JWT secret

Set `JWT_SECRET` in server environment.

- Local development supports loading from `.env`.
- Production/WildFly should use server environment variables.

## Deploy

Copy WAR to WildFly deployments directory or deploy via CLI:

```powershell
jboss-cli.bat --connect --command="deploy C:\path\to\blps.war --force"
```

## Deploy without JNDI datasource (for limited permissions)

If you cannot register PostgreSQL driver/datasource in WildFly, use application-managed datasource:

- Do not enable `wildfly` Spring profile.
- Provide DB settings as environment variables before starting WildFly:

```bash
export DB_URL='jdbc:postgresql://<host>:5432/<db>'
export DB_USER='<db_user>'
export DB_PASSWORD='<db_password>'
export JPA_DDL_AUTO='validate'
export JWT_SECRET='<strong_secret>'
```

In this mode PostgreSQL JDBC driver is loaded from the application WAR dependencies automatically.
