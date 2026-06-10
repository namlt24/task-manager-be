package com.taskmanager.user.repository;

import com.taskmanager.user.entity.Role;
import com.taskmanager.user.entity.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(RoleName name);
}
