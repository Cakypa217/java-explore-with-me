package ru.practicum.controller.priv;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.model.dto.comment.CommentDto;
import ru.practicum.model.dto.comment.NewCommentDto;
import ru.practicum.model.dto.comment.UpdateCommentDto;
import ru.practicum.service.comment.CommentService;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/users/{userId}/events/{eventId}/comments")
public class PrivateCommentController {
    private final CommentService commentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommentDto createComment(@PathVariable Long userId, @PathVariable Long eventId,
                                    @Valid @RequestBody NewCommentDto newCommentDto) {
        return commentService.createComment(userId, eventId, newCommentDto);
    }

    @GetMapping
    public List<CommentDto> getCommentsByEvent(@PathVariable Long userId, @PathVariable Long eventId) {
        return commentService.getCommentsByEvent(eventId);
    }

    @GetMapping("/{commentId}")
    public CommentDto getCommentById(@PathVariable Long userId, @PathVariable Long commentId) {
        return commentService.getCommentById(commentId);
    }

    @PatchMapping("/{commentId}")
    public CommentDto updateComment(@PathVariable Long userId, @PathVariable Long commentId,
                                    @Valid @RequestBody UpdateCommentDto updateCommentDto) {
        return commentService.updateComment(userId, commentId, updateCommentDto);
    }

    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@PathVariable Long userId, @PathVariable Long commentId) {
        commentService.deleteComment(userId, commentId);
    }
}