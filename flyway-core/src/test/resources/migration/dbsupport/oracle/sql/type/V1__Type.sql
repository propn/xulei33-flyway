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

CREATE TYPE full_name_type AS OBJECT
( FirstName       VARCHAR2(80),
  MiddleName      VARCHAR2(80),
  LastName        VARCHAR2(80) );

CREATE TYPE full_mailing_address_type AS OBJECT
( full_name    full_name_type,
  Street       VARCHAR2(80),
  City         VARCHAR2(80),
  State        CHAR(2));

CREATE TABLE customer (
  full_address  full_mailing_address_type
);

INSERT INTO customer VALUES (
  full_mailing_address_type(
    full_name_type('John', 'F', 'Kennedy'),
    'Whitehouse plaza',
    'Washington',
    'DC'
  )
);