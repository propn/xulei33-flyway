--
-- Copyright (C) 2010-2011 the original author or authors.
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--         http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--

CREATE DATABASE flyway_db;
GO

CREATE DATABASE flyway_db_case_sensitive COLLATE SQL_Latin1_General_CP1_CS_AS;
GO

USE flyway_db;
GO

CREATE SCHEMA flyway_1;
GO

CREATE SCHEMA flyway_2;
GO

CREATE SCHEMA flyway_3;
GO
