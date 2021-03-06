create table if not exists `user` (
    `CREATED_AT` timestamp default current_timestamp,
    `UPDATED_AT` timestamp null,
    `ID` integer primary key autoincrement,
    `ENABLED` integer not null default 1,
    `NAME` varchar(63) not null,
    `DESCRIPTION` varchar(1023) null,
    `PASSWORD` varchar(127) not null,
    `SALT` varchar(127) not null
);

create table if not exists `application` (
    `CREATED_AT` timestamp default current_timestamp,
    `UPDATED_AT` timestamp null,
    `ID` integer primary key autoincrement,
    `ENABLED` integer not null default 1,
    `NAME` varchar(63) not null,
    `DESCRIPTION` varchar(1023) null,
    `APP_ID` varchar(24) unique not null,
    `API_KEY` varchar(255) not null
);

create table if not exists `issuer` (
    `CREATED_AT` timestamp default current_timestamp,
    `UPDATED_AT` timestamp null,
    `ID` integer primary key autoincrement,
    `UUID` varchar(63) unique not null,
    `ENABLED` integer not null default 1,
    `NAME` varchar(4) not null,
    `DESCRIPTION` varchar(1023) null,
    `PUBLIC_KEY_BASE64` varchar(1023) not null,
    `PRIVATE_KEY_BASE64` varchar(1023) null
);

create table if not exists `device` (
    `CREATED_AT` timestamp default current_timestamp,
    `UPDATED_AT` timestamp null,
    `ID` integer primary key autoincrement,
    `ENABLED` integer not null default 1,
    `NAME` varchar(63) not null,
    `DESCRIPTION` varchar(1023) null,
    `PUBLIC_KEY_BASE64` varchar(1023) not null,
    `SERIAL_NUMBER` varchar(63) not null
);

create table if not exists `vehicle` (
    `CREATED_AT` timestamp default current_timestamp,
    `UPDATED_AT` timestamp null,
    `ID` integer primary key autoincrement,
    `ENABLED` integer not null default 1,
    `NAME` varchar(63) not null,
    `DESCRIPTION` varchar(1023) null,
    `PUBLIC_KEY_BASE64` varchar(1023) not null,
    `SERIAL_NUMBER` varchar(63) not null,
    `ISSUER_ID` bigint not null,
    foreign key (`ISSUER_ID`)
      references issuer(`ID`)
      on update cascade on delete restrict
);

create table if not exists `application_vehicle` (
    `CREATED_AT` timestamp default current_timestamp,
    `APPLICATION_ID` bigint not null,
    `VEHICLE_ID` bigint not null,
    foreign key (`APPLICATION_ID`)
      references application(`ID`)
      on update cascade on delete restrict,
    foreign key (`VEHICLE_ID`)
      references vehicle(`ID`)
      on update cascade on delete restrict,
    constraint `INDEX_APP_ID_VEHICLE_ID_UNIQUE` UNIQUE (`APPLICATION_ID`, `VEHICLE_ID`)
);

create table if not exists `device_certificate` (
    `CREATED_AT` timestamp default current_timestamp,
    `UPDATED_AT` timestamp null,
    `ID` integer primary key autoincrement,
    `UUID` varchar(63) unique not null,
    `DEVICE_ID` bigint unique not null,
    `ISSUER_ID` bigint not null,
    `APPLICATION_ID` bigint not null,
    `SIGNED_CERTIFICATE_BASE64` varchar(1023) not null,
    foreign key (`DEVICE_ID`)
      references device(`ID`)
      on update cascade on delete restrict,
    foreign key (`ISSUER_ID`)
      references issuer(`ID`)
      on update cascade on delete restrict,
    foreign key (`APPLICATION_ID`)
      references application(`ID`)
      on update cascade on delete restrict,
    constraint `INDEX_DEVICE_CERT_DEVICE_ID_APPLICATION_ID_UNIQUE` UNIQUE (`DEVICE_ID`, `APPLICATION_ID`)
);

create table if not exists `access_certificate` (
    `CREATED_AT` timestamp default current_timestamp,
    `UPDATED_AT` timestamp null,
    `ID` integer primary key autoincrement,
    `UUID` varchar(63) unique not null,
    `VALID_FROM` timestamp not null,
    `VALID_UNTIL` timestamp not null,
    `ISSUER_ID` bigint not null,
    `APPLICATION_ID` bigint not null,
    `VEHICLE_ID` bigint not null,
    `DEVICE_ID` bigint not null,
    `DEVICE_ACCESS_CERTIFICATE_BASE64` varchar(1023) not null,
    `DEVICE_ACCESS_CERTIFICATE_SIGNATURE_BASE64` varchar(1023) null,
    `SIGNED_DEVICE_ACCESS_CERTIFICATE_BASE64` varchar(2047) null,
    `VEHICLE_ACCESS_CERTIFICATE_BASE64` varchar(1023) not null,
    `VEHICLE_ACCESS_CERTIFICATE_SIGNATURE_BASE64` varchar(1023) null,
    `SIGNED_VEHICLE_ACCESS_CERTIFICATE_BASE64` varchar(2047) null,
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
      on update cascade on delete restrict
);
