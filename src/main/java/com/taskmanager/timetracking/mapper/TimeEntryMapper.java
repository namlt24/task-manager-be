package com.taskmanager.timetracking.mapper;

import com.taskmanager.timetracking.dto.TimeEntryDto;
import com.taskmanager.timetracking.entity.TimeEntry;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TimeEntryMapper {

    TimeEntryDto toDto(TimeEntry entry);
}
