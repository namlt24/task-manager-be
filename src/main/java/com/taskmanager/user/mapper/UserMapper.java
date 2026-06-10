package com.taskmanager.user.mapper;

import com.taskmanager.user.dto.UserDto;
import com.taskmanager.user.entity.Role;
import com.taskmanager.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Named;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @org.mapstruct.Mapping(target = "roles", source = "roles", qualifiedByName = "rolesToNames")
    UserDto toDto(User user);

    @Named("rolesToNames")
    default List<String> rolesToNames(Set<Role> roles) {
        if (roles == null) {
            return List.of();
        }
        return roles.stream().map(r -> r.getName().name()).toList();
    }
}
