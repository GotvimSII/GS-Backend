CREATE TABLE IF NOT EXISTS recipes (
    id SERIAL PRIMARY KEY,
    title TEXT NOT NULL,
    instructions TEXT,
    preparation_time INTEGER,
    title_vector TSVECTOR
);

CREATE TABLE IF NOT EXISTS ingredients (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS ingredient_aliases (
    alias VARCHAR(256) UNIQUE PRIMARY KEY,
    ingredient_id INTEGER REFERENCES ingredients(id)
);

CREATE TABLE IF NOT EXISTS recipes_ingredients (
    recipe_id INTEGER REFERENCES recipes(id),
    ingredient_id INTEGER REFERENCES ingredients(id),
    quantity DOUBLE PRECISION,
    unit VARCHAR(32),
    section VARCHAR(96),

    PRIMARY KEY(recipe_id, ingredient_id)
);