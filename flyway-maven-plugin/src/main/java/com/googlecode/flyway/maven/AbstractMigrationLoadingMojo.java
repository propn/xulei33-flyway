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
package com.googlecode.flyway.maven;

import com.googlecode.flyway.core.Flyway;
import com.googlecode.flyway.core.validation.ValidationErrorMode;
import com.googlecode.flyway.core.validation.ValidationMode;

/**
 * Base class for mojos that rely on loading migrations from the classpath.
 *
 * @phase pre-integration-test
 */
@SuppressWarnings({"UnusedDeclaration"})
abstract class AbstractMigrationLoadingMojo extends AbstractFlywayMojo {
    /**
     * The base package where the Java migrations are located. (default: db.migration) <br> Also configurable with Maven
     * or System Property: ${flyway.basePackage}
     *
     * @parameter expression="${flyway.basePackage}"
     */
    private String basePackage;

    /**
     * The base directory on the classpath where the Sql migrations are located. (default: db/migration)<br> Also
     * configurable with Maven or System Property: ${flyway.baseDir}
     *
     * @parameter expression="${flyway.baseDir}"
     */
    private String baseDir;

    /**
     * The encoding of Sql migrations. (default: UTF-8)<br> Also configurable with Maven or System Property:
     * ${flyway.encoding}
     *
     * @parameter expression="${flyway.encoding}"
     */
    private String encoding;

    /**
     * The file name prefix for Sql migrations (default: V) Also configurable with Maven or System Property:
     * ${flyway.sqlMigrationPrefix}
     *
     * @parameter expression="${flyway.sqlMigrationPrefix}"
     */
    private String sqlMigrationPrefix;

    /**
     * The file name suffix for Sql migrations (default: .sql) Also configurable with Maven or System Property:
     * ${flyway.sqlMigrationSuffix}
     *
     * @parameter expression="${flyway.sqlMigrationSuffix}"
     */
    private String sqlMigrationSuffix;

    /**
     * The action to take when validation fails.<br/> <br/> Possible values are:<br/> <br/> <b>FAIL</b> (default)<br/>
     * Throw an exception and fail.<br/> <br/> <b>CLEAN (Warning ! Do not use in produktion !)</b><br/> Cleans the
     * database.<br/> <br/> This is exclusively intended as a convenience for development. Even tough we strongly
     * recommend not to change migration scripts once they have been checked into SCM and run, this provides a way of
     * dealing with this case in a smooth manner. The database will be wiped clean automatically, ensuring that the next
     * migration will bring you back to the state checked into SCM.<br/> <br/> This property has no effect when
     * <i>validationMode</i> is set to <i>NONE</i>.<br/> <br/> Also configurable with Maven or System Property:
     * ${flyway.validationErrorMode}
     *
     * @parameter expression="${flyway.validationErrorMode}"
     */
    private String validationErrorMode;

    @Override
    protected void doExecute(Flyway flyway) throws Exception {
        if (basePackage != null) {
            flyway.setBasePackage(basePackage);
        }
        if (baseDir != null) {
            flyway.setBaseDir(baseDir);
        }
        if (encoding != null) {
            flyway.setEncoding(encoding);
        }
        if (sqlMigrationPrefix != null) {
            flyway.setSqlMigrationPrefix(sqlMigrationPrefix);
        }
        if (sqlMigrationSuffix != null) {
            flyway.setSqlMigrationSuffix(sqlMigrationSuffix);
        }
        if (validationErrorMode != null) {
            flyway.setValidationErrorMode(ValidationErrorMode.valueOf(validationErrorMode.toUpperCase()));
        }
    }
}
