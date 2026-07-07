package com.arjunsports.contentagent.modules.user;

import com.arjunsports.contentagent.common.dto.ApiResponse;
import com.arjunsports.contentagent.common.dto.PageResponse;
import com.arjunsports.contentagent.modules.user.dto.CreateUserRequest;
import com.arjunsports.contentagent.modules.user.dto.UpdateUserRequest;
import com.arjunsports.contentagent.modules.user.dto.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        return ApiResponse.success("User created", userService.create(request));
    }

    @GetMapping
    public ApiResponse<PageResponse<UserResponse>> list(Pageable pageable) {
        return ApiResponse.success(PageResponse.from(userService.list(pageable)));
    }

    @GetMapping("/{id}")
    public ApiResponse<UserResponse> get(@PathVariable UUID id) {
        return ApiResponse.success(userService.get(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<UserResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request) {
        return ApiResponse.success("User updated", userService.update(id, request));
    }
}
