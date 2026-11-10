alter table if exists sector_daily_detail
    rename column index_code to sector_code;

alter table if exists sector_daily_detail
    rename column index_name to sector_name;
