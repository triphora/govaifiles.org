CREATE TABLE countries (
    id int NOT NULL PRIMARY KEY,
    name varchar(100) NOT NULL,
    capital varchar(100) NOT NULL,
    gdp int NULL
);

INSERT INTO countries (id, name, capital, gdp) VALUES (0, 'United States', 'Washington, D.C.', 300);
INSERT INTO countries (id, name, capital, gdp) VALUES (1, 'United Kingdom', 'London', 200);
INSERT INTO countries (id, name, capital, gdp) VALUES (2, 'Canada', 'Ottawa', 100);
INSERT INTO countries (id, name, capital, gdp) VALUES (3, 'Patch Amberdash', 'Jacobian', 2);