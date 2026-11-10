-- Do not reset an existing sequence: production may have allocated values beyond the table maximum.
DO $$
DECLARE
    entry record;
    max_id bigint;
BEGIN
    FOR entry IN SELECT * FROM (VALUES
        ('batch_job_seq', 'batch_job_instance', 'job_instance_id'),
        ('batch_job_execution_seq', 'batch_job_execution', 'job_execution_id'),
        ('batch_step_execution_seq', 'batch_step_execution', 'step_execution_id')
    ) AS sequences(sequence_name, table_name, id_column)
    LOOP
        IF to_regclass(entry.sequence_name) IS NULL THEN
            EXECUTE format('CREATE SEQUENCE %I', entry.sequence_name);
            EXECUTE format('SELECT max(%I) FROM %I', entry.id_column, entry.table_name) INTO max_id;
            IF max_id IS NOT NULL THEN
                PERFORM setval(entry.sequence_name::regclass, GREATEST(max_id, 1), true);
            END IF;
        END IF;
    END LOOP;
END $$;
