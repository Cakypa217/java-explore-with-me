package ru.practicum.mapper;

import org.mapstruct.*;
import ru.practicum.model.dto.comment.CommentDto;
import ru.practicum.model.dto.comment.NewCommentDto;
import ru.practicum.model.dto.comment.UpdateCommentDto;
import ru.practicum.model.entity.Comment;

@Mapper(componentModel = "spring", uses = UserMapper.class)
public interface CommentMapper {

    @Mapping(source = "event.id", target = "eventId")
    CommentDto toDto(Comment comment);

    Comment toEntity(NewCommentDto dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(UpdateCommentDto dto, @MappingTarget Comment comment);
}