create table if not exists `user` (
    `CREATED_AT` timestamp not null default current_timestamp,
    `UPDATED_AT` timestamp null,
    `ID` bigint not null,
    `ENABLED` integer not null default 1,
    `NAME` varchar(63) not null,
    `PASSWORD` varchar(127) not null,
    `SALT` varchar(127) not null,
    primary key (`ID`)
);

create table if not exists `application` (
    `CREATED_AT` timestamp not null default current_timestamp,
    `UPDATED_AT` timestamp null,
    `ID` bigint not null,
    `ENABLED` integer not null default 1,
    `NAME` varchar(63) not null,
    `DESCRIPTION` varchar(1023) null,
    `APP_ID` varchar(24) not null,
    `API_KEY` varchar(255) not null,
    primary key (`ID`),
    unique key `INDEX_APP_APP_ID_UNIQUE` (`APP_ID`),
    unique key `INDEX_APP_API_KEY_UNIQUE` (`API_KEY`)
);

create table if not exists `issuer` (
    `CREATED_AT` timestamp not null default current_timestamp,
    `UPDATED_AT` timestamp null,
    `ID` bigint not null,
    `ENABLED` integer not null default 1,
    `NAME` varchar(4) not null,
    `DESCRIPTION` varchar(1023) null,
    `PUBLIC_KEY_BASE64` varchar(1023) not null,
    `PRIVATE_KEY_BASE64` varchar(1023) not null,
    primary key (`ID`)
);

create table if not exists `device` (
    `CREATED_AT` timestamp not null default current_timestamp,
    `UPDATED_AT` timestamp null,
    `ID` bigint not null,
    `ENABLED` integer not null default 1,
    `NAME` varchar(63) not null,
    `DESCRIPTION` varchar(1023) null,
    `PUBLIC_KEY_BASE64` varchar(1023) not null,
    `SERIAL_NUMBER` varchar(63) not null,
    primary key (`ID`)
);

create table if not exists `vehicle` (
    `CREATED_AT` timestamp not null default current_timestamp,
    `UPDATED_AT` timestamp null,
    `ID` bigint not null,
    `ENABLED` integer not null default 1,
    `NAME` varchar(63) not null,
    `DESCRIPTION` varchar(1023) null,
    `PUBLIC_KEY_BASE64` varchar(1023) not null,
    `SERIAL_NUMBER` varchar(63) not null,
    `ISSUER_ID` bigint not null,
    primary key (`ID`),
    foreign key (`ISSUER_ID`)
    references issuer(`ID`)
    on update cascade on delete restrict
);

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
);

create table if not exists `device_certificate` (
    `CREATED_AT` timestamp not null default current_timestamp,
    `UPDATED_AT` timestamp null,
    `ID` bigint not null,
    `UUID` varchar(63) not null,
    `SIGNED_CERTIFICATE_BASE64` varchar(1023) not null,
    `DEVICE_ID` bigint not null,
    `ISSUER_ID` bigint not null,
    `APPLICATION_ID` bigint not null,
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
);

create table if not exists `access_certificate` (
    `CREATED_AT` timestamp not null default current_timestamp,
    `UPDATED_AT` timestamp null,
    `ID` bigint not null,
    `UUID` varchar(63) not null,
    `SIGNED_VEHICLE_ACCESS_CERTIFICATE_BASE64` varchar(1023) not null,
    `SIGNED_DEVICE_ACCESS_CERTIFICATE_BASE64` varchar(1023) not null,
    `VALID_FROM` timestamp not null,
    `VALID_UNTIL` timestamp not null,
    `ISSUER_ID` bigint not null,
    `APPLICATION_ID` bigint not null,
    `VEHICLE_ID` bigint not null,
    `DEVICE_ID` bigint not null,
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
);
