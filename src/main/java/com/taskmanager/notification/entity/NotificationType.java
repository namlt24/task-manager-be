package com.taskmanager.notification.entity;

/** Loại thông báo trong ứng dụng. */
public enum NotificationType {
    REMINDER,    // task tới hạn nhắc nhở
    RECURRENCE,  // sinh instance lặp lại (dự phòng cho mở rộng)
    ASSIGNED,    // được giao việc (M4)
    COMPLETED    // việc được hoàn thành (báo cho người tạo) (M4)
}
