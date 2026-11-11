DO $$ DECLARE n integer; BEGIN
 SELECT count(*) INTO n FROM information_schema.tables WHERE table_schema='public' AND table_type='BASE TABLE';
 IF n<>37 THEN RAISE EXCEPTION 'Expected 37 tables, found %',n; END IF;
 IF to_regclass('public.flyway_schema_history') IS NOT NULL THEN RAISE EXCEPTION 'Unexpected Flyway metadata'; END IF;
 IF to_regclass('public.batch_job_instance') IS NULL OR to_regclass('public.batch_step_execution_context') IS NULL THEN RAISE EXCEPTION 'Spring Batch schema incomplete'; END IF;
END $$;
SELECT current_database(),current_user,current_setting('server_version');
SELECT table_name FROM information_schema.tables WHERE table_schema='public' AND table_type='BASE TABLE' ORDER BY table_name;
