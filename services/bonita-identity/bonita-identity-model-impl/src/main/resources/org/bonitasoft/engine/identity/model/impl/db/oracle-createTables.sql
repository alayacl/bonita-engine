CREATE TABLE group_ (
  tenantid NUMBER(19, 0) NOT NULL,
  id NUMBER(19, 0) NOT NULL,
  name VARCHAR2(50) NOT NULL,
  parentPath VARCHAR2(50),
  displayName VARCHAR2(75),
  description VARCHAR2(1024),
  iconName VARCHAR2(50),
  iconPath VARCHAR2(50),
  createdBy NUMBER(19, 0),
  creationDate NUMBER(19, 0),
  lastUpdate NUMBER(19, 0),
  UNIQUE (tenantid, parentPath, name),
  PRIMARY KEY (tenantid, id)
);

CREATE TABLE role (
  tenantid NUMBER(19, 0) NOT NULL,
  id NUMBER(19, 0) NOT NULL,
  name VARCHAR2(50) NOT NULL,
  displayName VARCHAR2(75),
  description VARCHAR2(1024),
  iconName VARCHAR2(50),
  iconPath VARCHAR2(50),
  createdBy NUMBER(19, 0),
  creationDate NUMBER(19, 0),
  lastUpdate NUMBER(19, 0),
  UNIQUE (tenantid, name),
  PRIMARY KEY (tenantid, id)
);


CREATE TABLE user_ (
  tenantid NUMBER(19, 0) NOT NULL,
  id NUMBER(19, 0) NOT NULL,
  enabled NUMBER(1) NOT NULL,
  userName VARCHAR2(50) NOT NULL,
  password VARCHAR2(60),
  firstName VARCHAR2(50),
  lastName VARCHAR2(50),
  title VARCHAR2(50),
  jobTitle VARCHAR2(50),
  managerUserId NUMBER(19, 0),
  delegeeUserName VARCHAR2(50),
  iconName VARCHAR2(50),
  iconPath VARCHAR2(50),
  createdBy NUMBER(19, 0),
  creationDate NUMBER(19, 0),
  lastUpdate NUMBER(19, 0),
  lastConnection NUMBER(19, 0),
  UNIQUE (tenantid, userName),
  PRIMARY KEY (tenantid, id)
);


CREATE TABLE user_contactinfo (
  tenantid NUMBER(19, 0) NOT NULL,
  id NUMBER(19, 0) NOT NULL,
  userId NUMBER(19, 0) NOT NULL,
  email VARCHAR2(50),
  phone VARCHAR2(50),
  mobile VARCHAR2(50),
  fax VARCHAR2(50),
  building VARCHAR2(50),
  room VARCHAR2(50),
  address VARCHAR2(50),
  zipCode VARCHAR2(50),
  city VARCHAR2(50),
  state VARCHAR2(50),
  country VARCHAR2(50),
  website VARCHAR2(50),
  personal NUMBER(1)  NOT NULL,
  UNIQUE (tenantid, userId, personal),
  PRIMARY KEY (tenantid, id)
);
ALTER TABLE user_contactinfo ADD CONSTRAINT fk_contact_user FOREIGN KEY (tenantid, userId) REFERENCES user_ (tenantid, id) ON DELETE CASCADE;


CREATE TABLE p_metadata_def (
  tenantid NUMBER(19, 0) NOT NULL,
  id NUMBER(19, 0) NOT NULL,
  name VARCHAR2(50) NOT NULL,
  displayName VARCHAR2(75),
  description VARCHAR2(1024),
  UNIQUE (tenantid, name),
  PRIMARY KEY (tenantid, id)
);


CREATE TABLE p_metadata_val (
  tenantid NUMBER(19, 0) NOT NULL,
  metadataName VARCHAR2(50) NOT NULL,
  userName VARCHAR2(50) NOT NULL,
  value VARCHAR2(50),
  PRIMARY KEY (tenantid, metadataName, userName)
);

CREATE TABLE user_membership (
  tenantid NUMBER(19, 0) NOT NULL,
  id NUMBER(19, 0) NOT NULL,
  userId NUMBER(19, 0) NOT NULL,
  roleId NUMBER(19, 0) NOT NULL,
  groupId NUMBER(19, 0) NOT NULL,
  assignedBy NUMBER(19, 0),
  assignedDate NUMBER(19, 0),
  UNIQUE (tenantid, userId, roleId, groupId),
  PRIMARY KEY (tenantid, id)
);
