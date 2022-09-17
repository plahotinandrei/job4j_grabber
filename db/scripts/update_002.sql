create table if not exists post(
    id serial primary key,
    name text,
    link varchar(100) unique,
    text text,
    created timestamp
);