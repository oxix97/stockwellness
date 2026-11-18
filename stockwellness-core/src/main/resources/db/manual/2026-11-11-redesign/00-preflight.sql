-- Run only against an explicitly approved, newly created application database.
DO $$ BEGIN
  IF current_database() = 'postgres' THEN RAISE EXCEPTION 'Refusing maintenance database'; END IF;
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public')
    THEN RAISE EXCEPTION 'Target public schema is not empty'; END IF;
END $$;
