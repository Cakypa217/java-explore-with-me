package ru.practicum.service.comment;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.ForbiddenException;
import ru.practicum.mapper.CommentMapper;
import ru.practicum.model.dto.comment.CommentDto;
import ru.practicum.model.dto.comment.NewCommentDto;
import ru.practicum.model.dto.comment.UpdateCommentDto;
import ru.practicum.model.entity.Comment;
import ru.practicum.model.entity.Event;
import ru.practicum.model.entity.User;
import ru.practicum.model.enums.EventState;
import ru.practicum.repository.CommentRepository;
import ru.practicum.service.event.interfaces.PrivateEventService;
import ru.practicum.service.user.UserService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {
    private final CommentRepository commentRepository;
    private final UserService userService;
    private final PrivateEventService privateEventService;
    private final CommentMapper commentMapper;

    @Override
    @Transactional
    public CommentDto createComment(Long userId, Long eventId, NewCommentDto newCommentDto) {
        log.info("Запрос на создание комментария {} от пользователя с id = {} к событию с id = {} ",
                newCommentDto, userId, eventId);

        User user = userService.findById(userId);
        Event event = privateEventService.findById(eventId);

        if (!event.getState().equals(EventState.PUBLISHED)) {
            throw new ConflictException("Нельзя оставить комментарий к неопубликованному событию.");
        }

        Comment comment = commentMapper.toEntity(newCommentDto);
        comment.setUser(user);
        comment.setEvent(event);
        comment.setCreated(LocalDateTime.now());
        comment.setUpdated(LocalDateTime.now());

        comment = commentRepository.save(comment);
        CommentDto commentDto = commentMapper.toDto(comment);

        log.info("Комментарий {} создан", comment);
        return commentDto;
    }

    @Override
    public List<CommentDto> getCommentsByEvent(Long eventId) {
        log.info("Запрос на получение комментариев к событию с id = {}", eventId);

        List<CommentDto> comments = commentRepository.findByEventIdOrderByCreatedAsc(eventId)
                .stream()
                .map(commentMapper::toDto)
                .collect(Collectors.toList());

        log.info("Найдено {} комментариев для события с id = {}", comments.size(), eventId);
        return comments;
    }

    @Override
    public CommentDto getCommentById(Long commentId) {
        log.info("Запрос на получение комментария с id = {}", commentId);

        Comment comment = findById(commentId);
        CommentDto commentDto = commentMapper.toDto(comment);

        log.info("Найден комментарий {}", comment);
        return commentDto;
    }

    @Override
    @Transactional
    public CommentDto updateComment(Long userId, Long commentId, UpdateCommentDto updateCommentDto) {
        log.info("Запрос на обновление комментария с id = {} от пользователя с id = {}", commentId, userId);

        Comment comment = commentRepository.findByIdAndUserId(commentId, userId)
                .orElseThrow(() -> new ForbiddenException("Пользователь с id = " + userId
                        + " не является автором комментария с id = " + commentId));

        commentMapper.updateEntity(updateCommentDto, comment);
        comment = commentRepository.save(comment);
        CommentDto commentDto = commentMapper.toDto(comment);

        log.info("Комментарий {} обновлен", commentDto);
        return commentDto;
    }

    @Override
    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        log.info("Запрос на удаление комментария с id = {} от пользователя с id = {}", commentId, userId);

        Comment comment = findById(commentId);

        Long eventInitiatorId = comment.getEvent().getInitiator().getId();
        Long commentAuthorId = comment.getUser().getId();

        if (!userId.equals(commentAuthorId) && !userId.equals(eventInitiatorId)) {
            throw new ForbiddenException("Пользователь с id = " + userId +
                    " не является автором комментария или инициатором события и не может его удалить.");
        }

        commentRepository.delete(comment);
        log.info("Комментарий с id = {} удален пользователем с id = {}", commentId, userId);
    }

    @Override
    public Comment findById(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("Комментарий с id = " + commentId + " не найден."));
    }
}