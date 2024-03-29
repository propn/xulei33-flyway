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

CREATE USER flyway IDENTIFIED BY flyway;
CREATE USER flyway_1 IDENTIFIED BY flyway;
CREATE USER flyway_2 IDENTIFIED BY flyway;
CREATE USER flyway_3 IDENTIFIED BY flyway;
CREATE USER flyway_proxy IDENTIFIED BY flyway;
GRANT RESOURCE TO flyway_1;
GRANT RESOURCE TO flyway_2;
GRANT RESOURCE TO flyway_3;
GRANT all privileges TO flyway;
GRANT all privileges TO flyway_proxy;
GRANT create session TO flyway;
GRANT create session TO flyway_proxy;

ALTER USER flyway GRANT CONNECT THROUGH flyway_proxy;