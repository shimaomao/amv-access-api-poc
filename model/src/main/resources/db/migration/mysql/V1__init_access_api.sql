create table if not exists `user` (
    `CREATED_AT` timestamp not null default current_timestamp,
    `UPDATED_AT` timestamp null,
    `ID` bigint not null AUTO_INCREMENT,
    `ENABLED` integer not null default 1,
    `NAME` varchar(63) not null,
    `DESCRIPTION` varchar(1023) null,
    `PASSWORD` varchar(127) not null,
    `SALT` varchar(127) not null,
    primary key (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

create table if not exists `application` (
    `CREATED_AT` timestamp not null default current_timestamp,
    `UPDATED_AT` timestamp null,
    `ID` bigint not null AUTO_INCREMENT,
    `ENABLED` integer not null default 1,
    `NAME` varchar(63) not null,
    `DESCRIPTION` varchar(1023) null,
    `APP_ID` varchar(24) not null,
    `API_KEY` varchar(255) not null,
    primary key (`ID`),
    unique key `INDEX_APP_APP_ID_UNIQUE` (`APP_ID`),
    unique key `INDEX_APP_API_KEY_UNIQUE` (`API_KEY`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

create table if not exists `issuer` (
    `CREATED_AT` timestamp not null default current_timestamp,
    `UPDATED_AT` timestamp null,
    `ID` bigint not null AUTO_INCREMENT,
    `ENABLED` integer not null default 1,
    `NAME` varchar(4) not null,
    `DESCRIPTION` varchar(1023) null,
    `PUBLIC_KEY_BASE64` varchar(1023) not null,
    `PRIVATE_KEY_BASE64` varchar(1023) not null,
    primary key (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

create table if not exists `device` (
    `CREATED_AT` timestamp not null default current_timestamp,
    `UPDATED_AT` timestamp null,
    `ID` bigint not null AUTO_INCREMENT,
    `ENABLED` integer not null default 1,
    `SERIAL_NUMBER` varchar(63) not null,
    `NAME` varchar(63) not null,
    `DESCRIPTION` varchar(1023) null,
    `PUBLIC_KEY_BASE64` varchar(1023) not null,
    primary key (`ID`),
    unique key `INDEX_DEVICE_SERIAL_NUMBER_UNIQUE` (`SERIAL_NUMBER`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

create table if not exists `vehicle` (
    `CREATED_AT` timestamp not null default current_timestamp,
    `UPDATED_AT` timestamp null,
    `ID` bigint not null AUTO_INCREMENT,
    `ENABLED` integer not null default 1,
    `SERIAL_NUMBER` varchar(63) not null,
    `NAME` varchar(63) not null,
    `DESCRIPTION` varchar(1023) null,
    `ISSUER_ID` bigint not null,
    `PUBLIC_KEY_BASE64` varchar(1023) not null,
    primary key (`ID`),
    unique key `INDEX_VEHICLE_SERIAL_NUMBER_UNIQUE` (`SERIAL_NUMBER`),
    foreign key (`ISSUER_ID`)
      references issuer(`ID`)
      on update cascade on delete restrict
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

create table if not exists `application_vehicle` (
    `CREATED_AT` timestamp not null default current_timestamp,
    `APPLICATION_ID` bigint not null,
    `VEHICLE_ID` bigint not null,
    foreign key (`APPLICATION_ID`)
      references application(`ID`)
      on update cascade on delete restrict,
    foreign key (`VEHICLE_ID`)
      references vehicle(`ID`)
      on update cascade on delete restrict,
    unique key `INDEX_APP_ID_VEHICLE_ID_UNIQUE` (`APPLICATION_ID`, `VEHICLE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

create table if not exists `device_certificate` (
    `CREATED_AT` timestamp not null default current_timestamp,
    `UPDATED_AT` timestamp null,
    `ID` bigint not null AUTO_INCREMENT,
    `UUID` varchar(63) not null,
    `DEVICE_ID` bigint not null,
    `ISSUER_ID` bigint not null,
    `APPLICATION_ID` bigint not null,
    `SIGNED_CERTIFICATE_BASE64` varchar(1023) not null,
    primary key (`ID`),
    foreign key (`DEVICE_ID`)
      references device(`ID`)
      on update cascade on delete restrict,
    foreign key (`ISSUER_ID`)
      references issuer(`ID`)
      on update cascade on delete restrict,
    foreign key (`APPLICATION_ID`)
      references application(`ID`)
      on update cascade on delete restrict,
    key INDEX_DEVICE_ID (`DEVICE_ID`),
    unique key `INDEX_DEVICE_CERT_UUID_UNIQUE` (`UUID`),
    unique key `INDEX_DEVICE_CERT_DEVICE_ID_APPLICATION_ID_UNIQUE` (`DEVICE_ID`, `APPLICATION_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

create table if not exists `access_certificate` (
    `CREATED_AT` timestamp not null default current_timestamp,
    `UPDATED_AT` timestamp null,
    `ID` bigint not null AUTO_INCREMENT,
    `UUID` varchar(63) not null,
    `VALID_FROM` datetime not null,
    `VALID_UNTIL` datetime not null,
    `ISSUER_ID` bigint not null,
    `APPLICATION_ID` bigint not null,
    `VEHICLE_ID` bigint not null,
    `DEVICE_ID` bigint not null,
    `SIGNED_VEHICLE_ACCESS_CERTIFICATE_BASE64` varchar(1023) not null,
    `SIGNED_DEVICE_ACCESS_CERTIFICATE_BASE64` varchar(1023) not null,
    primary key (`ID`),
    foreign key (`ISSUER_ID`)
      references issuer(`ID`)
      on update cascade on delete restrict,
    foreign key (`APPLICATION_ID`)
      references application(`ID`)
      on update cascade on delete restrict,
    foreign key (`VEHICLE_ID`)
      references vehicle(`ID`)
      on update cascade on delete restrict,
    foreign key (`DEVICE_ID`)
      references device(`ID`)
      on update cascade on delete restrict,
    unique key `INDEX_ACCESS_CERT_UUID_UNIQUE` (`UUID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
