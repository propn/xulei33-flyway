/**
 * Copyright (C) 2009-2010 the original author or authors.
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

package com.googlecode.flyway.core.migration;

/**
 * Base migration for migrations that use the standard Flyway version +
 * description embedding in their name. These migrations have names like
 * 1_2__Description .
 */
public abstract class BaseMigration extends Migration {
	/**
	 * Initializes the version of this Migration based on this standard Flyway
	 * name.
	 * 
	 * @param migrationName The migration name in standard Flyway format <i>VERSION</i>__<i>DESCRIPTION</i>, e.g. 1_2__Description
	 */
	protected final void initVersion(String migrationName) {
		schemaVersion = extractSchemaVersion(migrationName);
        description = extractDescription(migrationName);
	}

	/**
	 * Extracts the schema version from a migration name formatted as
	 * 1_2__Description.
	 * 
	 * @param migrationName
	 *            The string to parse.
	 * @return The extracted schema version.
	 */
	/* private -> for testing */
    static SchemaVersion extractSchemaVersion(String migrationName) {
        String rawVersion;

		// Handle the description
		int descriptionPos = migrationName.indexOf("__");
		if (descriptionPos < 0) {
            rawVersion = migrationName;
        } else {
			rawVersion = migrationName.substring(0, descriptionPos);
		}

		return new SchemaVersion(rawVersion.replace("_", "."));
	}

    /**
     * Extracts the description from a migration name formatted as
     * 1_2__Description.
     *
     * @param migrationName
     *            The string to parse.
     * @return The extracted description.
     */
    /* private -> for testing */
    static String extractDescription(String migrationName) {
        String rawVersion;

        // Handle the description
        int descriptionPos = migrationName.indexOf("__");
        if (descriptionPos >= 0) {
            return migrationName.substring(descriptionPos + 2).replaceAll("_", " ");
        }

        return null;
    }
}
