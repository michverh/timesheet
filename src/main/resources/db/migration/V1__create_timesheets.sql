-- The client's own timesheet handed in with a timesheet, and what was read from it.
create table client_timesheet (
    id        bigint generated always as identity primary key,
    file_name varchar(255) not null,
    format    varchar(10)  not null,
    content   bytea        not null,
    reading   varchar(10)  not null check (reading in ('UNREAD', 'READ', 'UNREADABLE'))
);

-- The hours worked per day according to a client's timesheet that was read.
create table client_reported_hours (
    client_timesheet_id bigint  not null references client_timesheet (id) on delete cascade,
    day                 date    not null,
    minutes_worked      integer not null check (minutes_worked >= 0),
    primary key (client_timesheet_id, day)
);

-- One timesheet per professional and month, a draft or submitted.
create table timesheet (
    id                          bigint generated always as identity primary key,
    professional_email          varchar(320) not null,
    month                       date         not null check (extract(day from month) = 1),
    client_name                 varchar(255),
    project_name                varchar(255),
    registered_absences_unknown boolean      not null,
    submitted_on                date,
    previously_submitted_on     date,
    client_timesheet_id         bigint unique references client_timesheet (id),
    -- Counts the changes kept, to detect a change made to a timesheet that was changed after it was found.
    version                     bigint       not null,
    unique (professional_email, month),
    check (project_name is null or client_name is not null)
);

-- Exactly one entry for every day of a timesheet's month.
create table timesheet_day (
    id           bigint generated always as identity primary key,
    timesheet_id bigint  not null references timesheet (id) on delete cascade,
    day          date    not null,
    kilometres   integer not null check (kilometres >= 0),
    explanation  text,
    unique (timesheet_id, day)
);

-- The hours booked on a day, per category; categories without hours are left out.
create table timesheet_day_hours (
    timesheet_day_id bigint      not null references timesheet_day (id) on delete cascade,
    category         varchar(20) not null,
    minutes          integer     not null check (minutes > 0),
    primary key (timesheet_day_id, category)
);
