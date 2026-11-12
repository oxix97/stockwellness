-- Spring Batch 5.2 metadata schema for Spring Boot 3.4.1.
BEGIN;
CREATE TABLE batch_job_instance(job_instance_id bigint PRIMARY KEY,version bigint,job_name varchar(100) NOT NULL,job_key varchar(32) NOT NULL,CONSTRAINT job_inst_un UNIQUE(job_name,job_key));
CREATE TABLE batch_job_execution(job_execution_id bigint PRIMARY KEY,version bigint,job_instance_id bigint NOT NULL REFERENCES batch_job_instance,create_time timestamp NOT NULL,start_time timestamp,end_time timestamp,status varchar(10),exit_code varchar(2500),exit_message varchar(2500),last_updated timestamp);
CREATE TABLE batch_job_execution_params(job_execution_id bigint NOT NULL REFERENCES batch_job_execution,parameter_name varchar(100) NOT NULL,parameter_type varchar(100) NOT NULL,parameter_value varchar(2500),identifying char NOT NULL);
CREATE TABLE batch_step_execution(step_execution_id bigint PRIMARY KEY,version bigint NOT NULL,step_name varchar(100) NOT NULL,job_execution_id bigint NOT NULL REFERENCES batch_job_execution,create_time timestamp NOT NULL,start_time timestamp,end_time timestamp,status varchar(10),commit_count bigint,read_count bigint,filter_count bigint,write_count bigint,read_skip_count bigint,write_skip_count bigint,process_skip_count bigint,rollback_count bigint,exit_code varchar(2500),exit_message varchar(2500),last_updated timestamp);
CREATE TABLE batch_step_execution_context(step_execution_id bigint PRIMARY KEY REFERENCES batch_step_execution,short_context varchar(2500) NOT NULL,serialized_context text);
CREATE TABLE batch_job_execution_context(job_execution_id bigint PRIMARY KEY REFERENCES batch_job_execution,short_context varchar(2500) NOT NULL,serialized_context text);
CREATE SEQUENCE batch_job_seq START WITH 1 INCREMENT BY 1 MAXVALUE 9223372036854775807 NO CYCLE;
CREATE SEQUENCE batch_job_execution_seq START WITH 1 INCREMENT BY 1 MAXVALUE 9223372036854775807 NO CYCLE;
CREATE SEQUENCE batch_step_execution_seq START WITH 1 INCREMENT BY 1 MAXVALUE 9223372036854775807 NO CYCLE;
COMMIT;
