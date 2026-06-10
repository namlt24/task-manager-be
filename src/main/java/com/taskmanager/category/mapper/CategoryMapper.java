package com.taskmanager.category.mapper;

import com.taskmanager.category.dto.CategoryDto;
import com.taskmanager.category.entity.Category;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    CategoryDto toDto(Category category);
}
