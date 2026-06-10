package com.taskmanager.subtask.mapper;

import com.taskmanager.subtask.dto.SubtaskDto;
import com.taskmanager.subtask.entity.Subtask;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SubtaskMapper {

    SubtaskDto toDto(Subtask subtask);
}
