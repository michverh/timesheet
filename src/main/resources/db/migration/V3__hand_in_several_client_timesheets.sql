-- A timesheet can have several client's timesheets handed in alongside, typically one per week.
alter table client_timesheet add column timesheet_id bigint references timesheet (id) on delete cascade;

update client_timesheet
set timesheet_id = timesheet.id
from timesheet
where timesheet.client_timesheet_id = client_timesheet.id;

delete from client_timesheet where timesheet_id is null;

alter table client_timesheet alter column timesheet_id set not null;
alter table timesheet drop column client_timesheet_id;
create index client_timesheet_timesheet_id on client_timesheet (timesheet_id);
