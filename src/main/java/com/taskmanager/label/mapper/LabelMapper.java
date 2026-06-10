package com.taskmanager.label.mapper;

import com.taskmanager.label.dto.LabelDto;
import com.taskmanager.label.entity.Label;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface LabelMapper {

    LabelDto toDto(Label label);
}
