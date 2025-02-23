package ru.practicum.service.comment;

import ru.practicum.model.dto.comment.CommentDto;
import ru.practicum.model.dto.comment.NewCommentDto;
import ru.practicum.model.dto.comment.UpdateCommentDto;
import ru.practicum.model.entity.Comment;

import java.util.List;

public interface CommentService {

    CommentDto createComment(Long userId, Long eventId, NewCommentDto newCommentDto);

    List<CommentDto> getCommentsByEvent(Long eventId);

    CommentDto getCommentById(Long commentId);

    CommentDto updateComment(Long userId, Long commentId, UpdateCommentDto updateCommentDto);

    void deleteComment(Long userId, Long commentId);

    Comment findById(Long commentId);
}