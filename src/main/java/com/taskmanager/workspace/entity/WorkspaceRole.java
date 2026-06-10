package com.taskmanager.workspace.entity;

/** Vai trò của một thành viên trong workspace. */
public enum WorkspaceRole {
    OWNER,    // người tạo: toàn quyền, quản lý thành viên/vai trò, xoá workspace
    MANAGER,  // project manager: quản lý board/task, giao việc, mời thành viên
    MEMBER    // thành viên: làm việc trên task được giao
}
