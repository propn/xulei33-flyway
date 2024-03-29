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
import com.googlecode.flyway.core.migration.SchemaVersion;

/**
 * Maven goal that initializes the metadata table in an existing schema.
 *
 * @goal init
 * @since 0.8.5
 */
@SuppressWarnings({"UnusedDeclaration", "JavaDoc"})
public class InitMojo extends AbstractFlywayMojo {
    /**
     * The initial version to put in the database. (default: 0) <br> default property: ${flyway.initialVersion}
     *
     * @parameter expression="${flyway.initialVersion}"
     */
    private String initialVersion;

    /**
     * The description of the initial version. (default: << Flyway Init >>)<br> default property:
     * ${flyway.initialDescription}
     *
     * @parameter expression="${flyway.initialDescription}"
     */
    private String initialDescription;

    @Override
    protected void doExecute(Flyway flyway) throws Exception {
        if (initialVersion != null) {
            flyway.setInitialVersion(new SchemaVersion(initialVersion));
        }
        if (initialDescription != null) {
            flyway.setInitialDescription(initialDescription);
        }

        flyway.init();
    }
}