# --- !Ups

create table "_FILE_" ("ID" SERIAL NOT NULL PRIMARY KEY,"TEMPLATE_ID" VARCHAR(254) NOT NULL,"TEMPLATE_DATE" TIMESTAMP NOT NULL,"CONTENT" BYTEA NOT NULL,"NAME" VARCHAR(254) NOT NULL,"DATE" TIMESTAMP NOT NULL);
create table "TEMPLATE" ("ID" VARCHAR(254) NOT NULL,"CONTENT" BYTEA NOT NULL,"DATE" TIMESTAMP NOT NULL,"DESCRIPTION" VARCHAR(254),"VERSION" VARCHAR(254));
alter table "TEMPLATE" add constraint "PK" primary key("ID","DATE");
create index "INDEX_ID" on "TEMPLATE" ("ID");
create index "INDEX_DATE" on "TEMPLATE" ("ID");
alter table "_FILE_" add constraint "TEMPLATE_ID_DATE_FK" foreign key("TEMPLATE_ID","TEMPLATE_DATE") references "TEMPLATE"("ID","DATE") on update CASCADE on delete CASCADE;

# --- !Downs

alter table "_FILE_" drop constraint "TEMPLATE_ID_DATE_FK";
drop table "_FILE_";
alter table "TEMPLATE" drop constraint "PK";
drop table "TEMPLATE";

