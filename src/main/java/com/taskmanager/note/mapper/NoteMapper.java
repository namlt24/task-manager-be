package com.taskmanager.note.mapper;

import com.taskmanager.note.dto.NoteDto;
import com.taskmanager.note.entity.Note;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NoteMapper {

    NoteDto toDto(Note note);
}
