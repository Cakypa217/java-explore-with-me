package ru.practicum.service.user;

import ru.practicum.model.dto.user.NewUserRequest;
import ru.practicum.model.dto.user.UserDto;
import ru.practicum.model.entity.User;

import java.util.List;

public interface UserService {

    List<UserDto> getUsers(List<Long> ids, Integer from, Integer size);

    UserDto createUser(NewUserRequest newUserRequest);

    void deleteUser(Long userId);

    User findById(Long userId);
}