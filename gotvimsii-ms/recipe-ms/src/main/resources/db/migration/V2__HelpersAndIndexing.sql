CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE FUNCTION recipes_tsvector_update() RETURNS trigger AS $$
BEGIN
    NEW.title_vector := to_tsvector('simple', NEW.title);
    RETURN NEW;
END
$$ LANGUAGE plpgsql;

CREATE TRIGGER tsvectorupdate
BEFORE INSERT OR UPDATE ON recipes
FOR EACH ROW EXECUTE FUNCTION recipes_tsvector_update();

CREATE INDEX idx_recipes_title_trgm
ON recipes USING gin (title gin_trgm_ops);

CREATE INDEX idx_recipes_title_vector
ON recipes USING gin (title_vector);

CREATE INDEX idx_ingredient_aliases_alias_trgm
ON ingredient_aliases USING gin (alias gin_trgm_ops);

CREATE INDEX idx_recipes_ingredients_recipe
ON recipes_ingredients (recipe_id);

CREATE INDEX idx_recipes_ingredients_ingredient
ON recipes_ingredients (ingredient_id);