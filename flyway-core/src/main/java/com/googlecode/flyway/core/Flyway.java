/**
 * Copyright (C) 2010-2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.googlecode.flyway.core;

import com.googlecode.flyway.core.clean.DbCleaner;
import com.googlecode.flyway.core.dbsupport.DbSupport;
import com.googlecode.flyway.core.dbsupport.DbSupportFactory;
import com.googlecode.flyway.core.exception.FlywayException;
import com.googlecode.flyway.core.init.DbInit;
import com.googlecode.flyway.core.metadatatable.MetaDataTable;
import com.googlecode.flyway.core.metadatatable.MetaDataTableRow;
import com.googlecode.flyway.core.migration.DbMigrator;
import com.googlecode.flyway.core.migration.Migration;
import com.googlecode.flyway.core.migration.MigrationProvider;
import com.googlecode.flyway.core.migration.SchemaVersion;
import com.googlecode.flyway.core.validation.DbValidator;
import com.googlecode.flyway.core.validation.ValidationErrorMode;
import com.googlecode.flyway.core.validation.ValidationException;
import com.googlecode.flyway.core.validation.ValidationMode;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.sql.Driver;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * This is the centre point of Flyway, and for most users, the only class they will ever have to deal with.
 * <p/>
 * It is THE public API from which all important Flyway functions such as clean, validate and migrate can be called.
 */
public class Flyway {
    /**
     * Logger.
     */
    private static final Log LOG = LogFactory.getLog(Flyway.class);

    /**
     * Property name prefix for placeholders that are configured through properties.
     */
    private static final String PLACEHOLDERS_PROPERTY_PREFIX = "flyway.placeholders.";

    /**
     * The base package where the Java migrations are located. (default: db.migration)
     */
    private String basePackage = "db.migration";

    /**
     * The base directory on the classpath where the Sql migrations are located. (default: db/migration)
     */
    private String baseDir = "db/migration";

    /**
     * The encoding of Sql migrations. (default: UTF-8)
     */
    private String encoding = "UTF-8";

    /**
     * The schemas managed by Flyway. The first schema in the list will be the one containing the metadata table.
     * (default: The default schema for the datasource connection)
     */
    private String[] schemas = new String[0];

    /**
     * <p>The name of the schema metadata table that will be used by Flyway. (default: schema_version)</p><p> By default
     * (single-schema mode) the metadata table is placed in the default schema for the connection provided by the
     * datasource. </p> <p> When the <i>flyway.schemas</i> property is set (multi-schema mode), the metadata table is
     * placed in the first schema of the list. </p>
     */
    private String table = "schema_version";

    /**
     * The target version up to which Flyway should run migrations. Migrations with a higher version number will not be
     * applied. (default: the latest version)
     */
    private SchemaVersion target = SchemaVersion.LATEST;

    /**
     * The map of <placeholder, replacementValue> to apply to sql migration scripts.
     */
    private Map<String, String> placeholders = new HashMap<String, String>();

    /**
     * The prefix of every placeholder. (default: ${ )
     */
    private String placeholderPrefix = "${";

    /**
     * The suffix of every placeholder. (default: } )
     */
    private String placeholderSuffix = "}";

    /**
     * The file name prefix for sql migrations. (default: V)
     */
    private String sqlMigrationPrefix = "V";

    /**
     * The file name suffix for sql migrations. (default: .sql)
     */
    private String sqlMigrationSuffix = ".sql";

    /**
     * Ignores failed future migrations when reading the metadata table. These are migrations that were performed by a
     * newer deployment of the application that are not yet available in this version. For example: we have migrations
     * available on the classpath up to version 3.0. The metadata table indicates that a migration to version 4.0
     * (unknown to us) has already been attempted and failed. Instead of bombing out (fail fast) with an exception, a
     * warning is logged and Flyway terminates normally. This is useful for situations where a database rollback is not
     * an option. An older version of the application can then be redeployed, even though a newer one failed due to a
     * bad migration. (default: false)
     */
    private boolean ignoreFailedFutureMigration;

    /**
     * The mode for validation. Only used for migrate. When using validate validationMode is always ALL. (default: NONE)
     */
    private ValidationMode validationMode = ValidationMode.NONE;

    /**
     * The error mode for validation.
     */
    private ValidationErrorMode validationErrorMode = ValidationErrorMode.FAIL;

    /**
     * The initial version to put in the database. Only used for init. (default: 0)
     */
    private SchemaVersion initialVersion = new SchemaVersion("0");

    /**
     * The description of the initial version. Only used for init. (default: << Flyway Init >>)
     */
    private String initialDescription = "<< Flyway Init >>";

    /**
     * Flag to disable the check that a non-empty schema has been properly initialized with init. This check ensures
     * Flyway does not migrate or clean the wrong database in case of a configuration mistake. Be careful when disabling
     * this! (default: false)
     */
    private boolean disableInitCheck;

    /**
     * The dataSource to use to access the database. Must have the necessary privileges to execute ddl.
     */
    private DataSource dataSource;

    /**
     * The transaction manager to use. By default a transaction manager will be automatically created for use with the
     * datasource.
     */
    private PlatformTransactionManager transactionManager;

    /**
     * JdbcTemplate with ddl manipulation access to the database.
     */
    private JdbcTemplate jdbcTemplate;

    /**
     * The transaction template to use.
     */
    private TransactionTemplate transactionTemplate;

    /**
     * Database-specific functionality.
     */
    private DbSupport dbSupport;

    /**
     * Retrieves the base package where the Java migrations are located.
     *
     * @return The base package where the Java migrations are located. (default: db.migration)
     */
    public String getBasePackage() {
        return basePackage;
    }

    /**
     * Retrieves the base directory on the classpath where the Sql migrations are located.
     *
     * @return The base directory on the classpath where the Sql migrations are located. (default: sql/location)
     */
    public String getBaseDir() {
        return baseDir;
    }

    /**
     * Retrieves the encoding of Sql migrations.
     *
     * @return The encoding of Sql migrations. (default: UTF-8)
     */
    public String getEncoding() {
        return encoding;
    }

    /**
     * Retrieves the schemas managed by Flyway. The first schema in the list will be the one containing the metadata
     * table.
     *
     * @return The schemas managed by Flyway. (default: The default schema for the datasource connection)
     */
    public String[] getSchemas() {
        return schemas;
    }

    /**
     * <p>Retrieves the name of the schema metadata table that will be used by Flyway.</p><p> By default (single-schema
     * mode) the metadata table is placed in the default schema for the connection provided by the datasource. </p> <p>
     * When the <i>flyway.schemas</i> property is set (multi-schema mode), the metadata table is placed in the first
     * schema of the list. </p>
     *
     * @return The name of the schema metadata table that will be used by flyway. (default: schema_version)
     */
    public String getTable() {
        return table;
    }

    /**
     * Retrieves the target version up to which Flyway should run migrations. Migrations with a higher version number
     * will not be applied.
     *
     * @return The target version up to which Flyway should run migrations. Migrations with a higher version number will
     *         not be applied. (default: the latest version)
     */
    public SchemaVersion getTarget() {
        return target;
    }

    /**
     * Retrieves the map of <placeholder, replacementValue> to apply to sql migration scripts.
     *
     * @return The map of <placeholder, replacementValue> to apply to sql migration scripts.
     */
    public Map<String, String> getPlaceholders() {
        return placeholders;
    }

    /**
     * Retrieves the prefix of every placeholder.
     *
     * @return The prefix of every placeholder. (default: ${ )
     */
    public String getPlaceholderPrefix() {
        return placeholderPrefix;
    }

    /**
     * Retrieves the suffix of every placeholder.
     *
     * @return The suffix of every placeholder. (default: } )
     */
    public String getPlaceholderSuffix() {
        return placeholderSuffix;
    }

    /**
     * Retrieves the file name prefix for sql migrations.
     *
     * @return The file name prefix for sql migrations. (default: V)
     */
    public String getSqlMigrationPrefix() {
        return sqlMigrationPrefix;
    }

    /**
     * Retrieves the file name suffix for sql migrations.
     *
     * @return The file name suffix for sql migrations. (default: .sql)
     */
    public String getSqlMigrationSuffix() {
        return sqlMigrationSuffix;
    }

    /**
     * Retrieves whether to ignore failed future migrations when reading the metadata table. These are migrations that
     * were performed by a newer deployment of the application that are not yet available in this version. For example:
     * we have migrations available on the classpath up to version 3.0. The metadata table indicates that a migration to
     * version 4.0 (unknown to us) has already been attempted and failed. Instead of bombing out (fail fast) with an
     * exception, a warning is logged and Flyway terminates normally. This is useful for situations where a database
     * rollback is not an option. An older version of the application can then be redeployed, even though a newer one
     * failed due to a bad migration.
     *
     * @return {@code true} to terminate normally and log a warning, {@code false} to fail fast with an exception.
     *         (default: false)
     */
    public boolean isIgnoreFailedFutureMigration() {
        return ignoreFailedFutureMigration;
    }

    /**
     * Retrieves the mode for validation. Only used for migrate. When using validate validationMode is always ALL.
     *
     * @return The mode for validation. (default: NONE)
     */
    public ValidationMode getValidationMode() {
        return validationMode;
    }

    /**
     * Retrieves the error mode for validation.
     *
     * @return The error mode for validation.
     */
    public ValidationErrorMode getValidationErrorMode() {
        return validationErrorMode;
    }

    /**
     * Retrieves the initial version to put in the database. Only used for init.
     *
     * @return The initial version to put in the database. (default: 0)
     */
    public SchemaVersion getInitialVersion() {
        return initialVersion;
    }

    /**
     * Retrieves the description of the initial version. Only used for init.
     *
     * @return The description of the initial version. (default: << Flyway Init >>)
     */
    public String getInitialDescription() {
        return initialDescription;
    }

    /**
     * Flag to disable the check that a non-empty schema has been properly initialized with init. This check ensures
     * Flyway does not migrate or clean the wrong database in case of a configuration mistake. Be careful when disabling
     * this!
     *
     * @return {@code true} if the check is disabled. {@code false} if it is active. (default: false)
     */
    public boolean isDisableInitCheck() {
        return disableInitCheck;
    }

    /**
     * Retrieves the dataSource to use to access the database. Must have the necessary privileges to execute ddl.
     *
     * @return The dataSource to use to access the database. Must have the necessary privileges to execute ddl.
     */
    public DataSource getDataSource() {
        return dataSource;
    }

    /**
     * Retrives the transaction manager to use. By default a transaction manager will be automatically created for use with the
     * datasource.
     *
     * @return The transaction manager to use. By default a transaction manager will be automatically created for use with the
     * datasource.
     */
    public PlatformTransactionManager getTransactionManager() {
        return transactionManager;
    }

    /**
     * Ignores failed future migrations when reading the metadata table. These are migrations that were performed by a
     * newer deployment of the application that are not yet available in this version. For example: we have migrations
     * available on the classpath up to version 3.0. The metadata table indicates that a migration to version 4.0
     * (unknown to us) has already been attempted and failed. Instead of bombing out (fail fast) with an exception, a
     * warning is logged and Flyway terminates normally. This is useful for situations where a database rollback is not
     * an option. An older version of the application can then be redeployed, even though a newer one failed due to a
     * bad migration. (default: false)
     *
     * @param ignoreFailedFutureMigration {@code true} to terminate normally and log a warning, {@code false} to fail
     *                                    fast with an exception.
     */
    public void setIgnoreFailedFutureMigration(boolean ignoreFailedFutureMigration) {
        this.ignoreFailedFutureMigration = ignoreFailedFutureMigration;
    }

    /**
     * Sets the mode for validation. Only used for migrate. When using validate validationMode is always ALL.
     *
     * @param validationMode The mode for validation. (default: NONE)
     */
    public void setValidationMode(ValidationMode validationMode) {
        this.validationMode = validationMode;
    }

    /**
     * Sets the error mode for validation.
     *
     * @param validationErrorMode The error mode for validation
     */
    public void setValidationErrorMode(ValidationErrorMode validationErrorMode) {
        this.validationErrorMode = validationErrorMode;
    }

    /**
     * Sets the base package where the migrations are located.
     *
     * @param basePackage The base package where the migrations are located. (default: db.migration)
     */
    public void setBasePackage(String basePackage) {
        this.basePackage = basePackage;
    }

    /**
     * Sets the base directory on the classpath where the Sql migrations are located.
     *
     * @param baseDir The base directory on the classpath where the Sql migrations are located. (default: sql/location)
     */
    public void setBaseDir(String baseDir) {
        this.baseDir = baseDir;
    }

    /**
     * Sets the encoding of Sql migrations.
     *
     * @param encoding The encoding of Sql migrations. (default: UTF-8)
     */
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    /**
     * Sets the schemas managed by Flyway. The first schema in the list will be the one containing the metadata table.
     *
     * @param schemas The schemas managed by Flyway. May not be {@code null}. Must contain at least one element.
     */
    public void setSchemas(String... schemas) {
        this.schemas = schemas;
    }

    /**
     * <p>Sets the name of the schema metadata table that will be used by Flyway.</p><p> By default (single-schema mode)
     * the metadata table is placed in the default schema for the connection provided by the datasource. </p> <p> When
     * the <i>flyway.schemas</i> property is set (multi-schema mode), the metadata table is placed in the first schema
     * of the list. </p>
     *
     * @param table The name of the schema metadata table that will be used by flyway. (default: schema_version)
     */
    public void setTable(String table) {
        this.table = table;
    }

    /**
     * Sets the target version up to which Flyway should run migrations. Migrations with a higher version number will
     * not be applied.
     *
     * @param target The target version up to which Flyway should run migrations. Migrations with a higher version
     *               number will not be applied. (default: the latest version)
     */
    public void setTarget(SchemaVersion target) {
        this.target = target;
    }

    /**
     * Sets the placeholders to replace in sql migration scripts.
     *
     * @param placeholders The map of <placeholder, replacementValue> to apply to sql migration scripts.
     */
    public void setPlaceholders(Map<String, String> placeholders) {
        this.placeholders = placeholders;
    }

    /**
     * Sets the prefix of every placeholder.
     *
     * @param placeholderPrefix The prefix of every placeholder. (default: ${ )
     */
    public void setPlaceholderPrefix(String placeholderPrefix) {
        this.placeholderPrefix = placeholderPrefix;
    }

    /**
     * Sets the suffix of every placeholder.
     *
     * @param placeholderSuffix The suffix of every placeholder. (default: } )
     */
    public void setPlaceholderSuffix(String placeholderSuffix) {
        this.placeholderSuffix = placeholderSuffix;
    }

    /**
     * Sets the file name prefix for sql migrations.
     *
     * @param sqlMigrationPrefix The file name prefix for sql migrations (default: V)
     */
    public void setSqlMigrationPrefix(String sqlMigrationPrefix) {
        this.sqlMigrationPrefix = sqlMigrationPrefix;
    }

    /**
     * Sets the file name suffix for sql migrations.
     *
     * @param sqlMigrationSuffix The file name suffix for sql migrations (default: .sql)
     */
    public void setSqlMigrationSuffix(String sqlMigrationSuffix) {
        this.sqlMigrationSuffix = sqlMigrationSuffix;
    }

    /**
     * Sets the datasource to use. Must have the necessary privileges to execute ddl.
     *
     * @param dataSource The datasource to use. Must have the necessary privileges to execute ddl.
     */
    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;

        if (transactionManager == null) {
            transactionManager = new DataSourceTransactionManager(dataSource);
        }
    }

    /**
     * Sets the transaction manager to use. By default a transaction manager will be automatically created for use with the
     * datasource.
     *
     * @param transactionManager The transaction manager to use. By default a transaction manager will be automatically created for use with the
     * datasource.
     */
    public void setTransactionManager(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    /**
     * Sets up the remaining things (DbSupport, JdbcTemplate, TransactionTemplate, ...) for Flyway to function properly.
     * Must be run <b>after</b> the properties have been set.
     *
     * @throws FlywayException when the datasource has not been configured.
     */
    /* private -> for testing*/
    void performSetup() throws FlywayException {
        if (dataSource == null) {
            throw new FlywayException("DataSource not set! Check your configuration!");
        }

        transactionTemplate = new TransactionTemplate(transactionManager);
        jdbcTemplate = new JdbcTemplate(dataSource);

        dbSupport = DbSupportFactory.createDbSupport(jdbcTemplate);
        if (schemas.length == 0) {
            setSchemas(dbSupport.getCurrentSchema());
        }

        if (schemas.length == 1) {
            LOG.debug("Schema: " + schemas[0]);
        } else {
            LOG.debug("Schemas: " + StringUtils.arrayToCommaDelimitedString(schemas));
        }
    }

    /**
     * The initial version to put in the database. Only used for init.
     *
     * @param initialVersion The initial version to put in the database. (default: 0)
     */
    public void setInitialVersion(SchemaVersion initialVersion) {
        this.initialVersion = initialVersion;
    }

    /**
     * The description of the initial version. Only used for init.
     *
     * @param initialDescription The description of the initial version. (default: << Flyway Init >>)
     */
    public void setInitialDescription(String initialDescription) {
        this.initialDescription = initialDescription;
    }

    /**
     * Flag to disable the check that a non-empty schema has been properly initialized with init. This check ensures
     * Flyway does not migrate or clean the wrong database in case of a configuration mistake. Be careful when disabling
     * this!
     *
     * @param disableInitCheck {@code true} if the check is disabled. {@code false} if it is active. (default: false)
     */
    public void setDisableInitCheck(boolean disableInitCheck) {
        this.disableInitCheck = disableInitCheck;
    }

    /**
     * Starts the database migration. All pending migrations will be applied in order.
     *
     * @return The number of successfully applied migrations.
     * @throws FlywayException Thrown when the migration failed.
     */
    public int migrate() throws FlywayException {
        performSetup();

        MigrationProvider migrationProvider =
                new MigrationProvider(basePackage, baseDir, encoding, sqlMigrationPrefix, sqlMigrationSuffix, placeholders, placeholderPrefix, placeholderSuffix);
        List<Migration> availableMigrations = migrationProvider.findAvailableMigrations();
        if (availableMigrations.isEmpty()) {
            return 0;
        }

        MetaDataTable metaDataTable = createMetaDataTable();

        doValidate();

        metaDataTable.createIfNotExists();

        DbMigrator dbMigrator =
                new DbMigrator(transactionTemplate, jdbcTemplate, dbSupport, metaDataTable, target, ignoreFailedFutureMigration);
        return dbMigrator.migrate(availableMigrations);
    }

    /**
     * Validate applied migration with classpath migrations to detect accidental changes.
     *
     * @throws FlywayException thrown when the validation failed.
     */
    public void validate() throws FlywayException {
        performSetup();

        validationMode = ValidationMode.ALL;
        doValidate();
    }

    /**
     * Performs the actual validation. All set up must have taken place beforehand.
     */
    private void doValidate() {
        MigrationProvider migrationProvider =
                new MigrationProvider(basePackage, baseDir, encoding, sqlMigrationPrefix, sqlMigrationSuffix, placeholders, placeholderPrefix, placeholderSuffix);
        List<Migration> availableMigrations = migrationProvider.findAvailableMigrations();

        MetaDataTable metaDataTable = createMetaDataTable();
        if (SchemaVersion.EMPTY.equals(metaDataTable.getCurrentSchemaVersion()) && !disableInitCheck) {
            for (String schema : schemas) {
                if (!dbSupport.isSchemaEmpty(schema)) {
                    throw new ValidationException("Found non-empty schema '" + schema
                            + "' without metadata table! Use init() first to initialize the metadata table.");
                }
            }
        }

        DbValidator dbValidator = new DbValidator(validationMode, metaDataTable);
        final String validationError = dbValidator.validate(availableMigrations);

        if (validationError != null) {
            final String msg = "Validate failed. Found differences between applied migrations and available migrations: " + validationError;
            if (ValidationErrorMode.CLEAN.equals(validationErrorMode)) {
                LOG.warn(msg + " running clean and migrate again.");
                clean();
            } else {
                throw new ValidationException(msg);
            }
        }
    }

    /**
     * Drops all objects (tables, views, procedures, triggers, ...) in the configured schemas.
     */
    public void clean() {
        performSetup();

        new DbCleaner(transactionTemplate, jdbcTemplate, dbSupport, schemas).clean();
    }

    /**
     * Returns the status (current version) of the database.
     *
     * @return The latest applied migration, or {@code null} if no migration has been applied yet.
     */
    public MetaDataTableRow status() {
        performSetup();

        MetaDataTable metaDataTable = createMetaDataTable();
        return metaDataTable.latestAppliedMigration();
    }

    /**
     * Returns the history (all applied migrations) of the database.
     *
     * @return All migrations applied to the database, sorted, oldest first. An empty list if none.
     */
    public List<MetaDataTableRow> history() {
        performSetup();

        MetaDataTable metaDataTable = createMetaDataTable();
        return metaDataTable.allAppliedMigrations();
    }

    /**
     * Creates and initializes the Flyway metadata table.
     *
     * @throws FlywayException when the schema initialization failed.
     */
    public void init() throws FlywayException {
        performSetup();

        MetaDataTable metaDataTable = createMetaDataTable();
        new DbInit(transactionTemplate, metaDataTable).init(initialVersion, initialDescription);
    }

    /**
     * @return A new, fully configured, MetaDataTable instance.
     */
    private MetaDataTable createMetaDataTable() {
        return new MetaDataTable(transactionTemplate, jdbcTemplate, dbSupport, schemas[0], table);
    }

    /**
     * Configures Flyway with these properties. This overwrites any existing configuration. Property names are
     * documented in the flyway maven plugin.
     *
     * @param properties Properties used for configuration.
     * @throws FlywayException when the configuration failed.
     */
    public void configure(Properties properties) {
        String driver = properties.getProperty("flyway.driver");
        String url = properties.getProperty("flyway.url");
        String user = properties.getProperty("flyway.user");
        String password = properties.getProperty("flyway.password");

        if (StringUtils.hasText(driver) && StringUtils.hasText(url) && StringUtils.hasText(user)
                && (password != null)) {
            // All datasource properties set
            Driver driverClazz;
            try {
                ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
                driverClazz = (Driver) Class.forName(driver, true, classLoader).newInstance();
            } catch (Exception e) {
                throw new FlywayException("Error instantiating database driver: " + driver, e);
            }

            setDataSource(new SimpleDriverDataSource(driverClazz, url, user, password));
        } else if (StringUtils.hasText(driver) || StringUtils.hasText(url) || StringUtils.hasText(user) || (password != null)) {
            // Some, but not all datasource properties set
            LOG.warn("Discarding INCOMPLETE dataSource configuration!" +
                    " At least one of flyway.driver, flyway.url, flyway.user or flyway.password missing.");
        }


        String baseDirProp = properties.getProperty("flyway.baseDir");
        if (baseDirProp != null) {
            setBaseDir(baseDirProp);
        }
        String placeholderPrefixProp = properties.getProperty("flyway.placeholderPrefix");
        if (placeholderPrefixProp != null) {
            setPlaceholderPrefix(placeholderPrefixProp);
        }
        String placeholderSuffixProp = properties.getProperty("flyway.placeholderSuffix");
        if (placeholderSuffixProp != null) {
            setPlaceholderSuffix(placeholderSuffixProp);
        }
        String sqlMigrationPrefixProp = properties.getProperty("flyway.sqlMigrationPrefix");
        if (sqlMigrationPrefixProp != null) {
            setSqlMigrationPrefix(sqlMigrationPrefixProp);
        }
        String sqlMigrationSuffixProp = properties.getProperty("flyway.sqlMigrationSuffix");
        if (sqlMigrationSuffixProp != null) {
            setSqlMigrationSuffix(sqlMigrationSuffixProp);
        }
        String basePackageProp = properties.getProperty("flyway.basePackage");
        if (basePackageProp != null) {
            setBasePackage(basePackageProp);
        }
        String encodingProp = properties.getProperty("flyway.encoding");
        if (encodingProp != null) {
            setEncoding(encodingProp);
        }
        String schemasProp = properties.getProperty("flyway.schemas");
        if (schemasProp != null) {
            setSchemas(StringUtils.tokenizeToStringArray(schemasProp, ","));
        }
        String tableProp = properties.getProperty("flyway.table");
        if (tableProp != null) {
            setTable(tableProp);
        }
        String validationErrorModeProp = properties.getProperty("flyway.validationErrorMode");
        if (validationErrorModeProp != null) {
            setValidationErrorMode(ValidationErrorMode.valueOf(validationErrorModeProp));
        }
        String validationModeProp = properties.getProperty("flyway.validationMode");
        if (validationModeProp != null) {
            setValidationMode(ValidationMode.valueOf(validationModeProp));
        }
        String initialVersionProp = properties.getProperty("flyway.initialVersion");
        if (initialVersionProp != null) {
            setInitialVersion(new SchemaVersion(initialVersionProp));
        }
        String initialDescriptionProp = properties.getProperty("flyway.initialDescription");
        if (initialDescriptionProp != null) {
            setInitialDescription(initialDescriptionProp);
        }
        String disableInitCheckProp = properties.getProperty("flyway.disableInitCheck");
        if (disableInitCheckProp != null) {
            setDisableInitCheck(Boolean.parseBoolean(disableInitCheckProp));
        }
        String targetProp = properties.getProperty("flyway.target");
        if (targetProp != null) {
            setTarget(new SchemaVersion(targetProp));
        }

        Map<String, String> placeholdersFromProps = new HashMap<String, String>();
        for (Object property : properties.keySet()) {
            String propertyName = (String) property;
            if (propertyName.startsWith(PLACEHOLDERS_PROPERTY_PREFIX)
                    && propertyName.length() > PLACEHOLDERS_PROPERTY_PREFIX.length()) {
                String placeholderName = propertyName.substring(PLACEHOLDERS_PROPERTY_PREFIX.length());
                String placeholderValue = properties.getProperty(propertyName);
                placeholdersFromProps.put(placeholderName, placeholderValue);
            }
        }
        setPlaceholders(placeholdersFromProps);
    }
}
