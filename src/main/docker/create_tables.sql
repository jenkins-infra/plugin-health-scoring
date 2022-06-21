CREATE DATABASE testdb;

CREATE TABLE plugins (
    id SERIAL PRIMARY KEY,
    name character varying(255) NOT NULL UNIQUE,
    scm character varying(255),
    release_timestamp timestamp with time zone NOT NULL
);