package ru.practicum.mapper;

import org.mapstruct.Mapper;
import ru.practicum.model.dto.user.NewUserRequest;
import ru.practicum.model.dto.user.UserDto;
import ru.practicum.model.dto.user.UserShortDto;
import ru.practicum.model.entity.User;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserDto toUserDto(User user);

    User toUser(NewUserRequest newUserRequest);

    UserShortDto toShortDto(User user);
}