package com.arjunsports.contentagent.modules.user;

import com.arjunsports.contentagent.modules.user.dto.CreateUserRequest;
import com.arjunsports.contentagent.modules.user.dto.UpdateUserRequest;
import com.arjunsports.contentagent.modules.user.dto.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface UserService {

    UserResponse create(CreateUserRequest request);

    UserResponse get(UUID id);

    Page<UserResponse> list(Pageable pageable);

    UserResponse update(UUID id, UpdateUserRequest request);
}
