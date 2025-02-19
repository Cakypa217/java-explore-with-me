package ru.practicum.service.user;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import ru.practicum.exception.ConflictException;
import ru.practicum.mapper.UserMapper;
import ru.practicum.model.dto.user.NewUserRequest;
import ru.practicum.model.dto.user.UserDto;
import ru.practicum.model.entity.User;
import ru.practicum.repository.UserRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public List<UserDto> getUsers(List<Long> ids, Integer from, Integer size) {
        log.info("Запрос на получение пользователей с параметрами: ids={}, from={}, size={}", ids, from, size);

        PageRequest pageRequest = PageRequest.of(from / size, size);

        Page<User> usersPage = (ids == null || ids.isEmpty())
                ? userRepository.findAll(pageRequest)
                : userRepository.findAllByIdIn(ids, pageRequest);

        List<UserDto> userDtos = usersPage.getContent()
                .stream()
                .map(userMapper::toUserDto)
                .toList();

        log.info("Найдено {} пользователей", userDtos.size());
        return userDtos;
    }

    @Override
    public UserDto createUser(NewUserRequest newUserRequest) {
        log.info("Запрос на создание пользователя {}", newUserRequest);
        try {

            User user = userMapper.toUser(newUserRequest);
            User savedUser = userRepository.save(user);
            UserDto userDto = userMapper.toUserDto(savedUser);

            log.info("Создан пользователь {}", userDto);
            return userDto;
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Пользователь с таким email уже зарегистрирован");
        }
    }

    @Override
    public void deleteUser(Long userId) {
        log.info("Получен запрос на удаление пользователя с id {}", userId);

        findById(userId);
        userRepository.deleteById(userId);

        log.info("Пользователь с id {} удален", userId);
    }

    @Override
    public User findById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь c id " + userId + " не найден"));
    }
}