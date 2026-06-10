package com.taskmanager.task.mapper;

import com.taskmanager.label.mapper.LabelMapper;
import com.taskmanager.task.dto.TaskDto;
import com.taskmanager.task.entity.Task;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = { LabelMapper.class })
public interface TaskMapper {

    @Mapping(target = "subtaskTotal", source = "subtaskTotal")
    @Mapping(target = "subtaskDone", source = "subtaskDone")
    @Mapping(target = "attachmentCount", source = "attachmentCount")
    @Mapping(target = "trackedSeconds", source = "trackedSeconds")
    TaskDto toDto(Task task, int subtaskTotal, int subtaskDone, int attachmentCount, long trackedSeconds);
}
