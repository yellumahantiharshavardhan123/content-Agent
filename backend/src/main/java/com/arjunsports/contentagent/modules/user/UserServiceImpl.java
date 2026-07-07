package com.arjunsports.contentagent.modules.user;

import com.arjunsports.contentagent.common.audit.ActivityAction;
import com.arjunsports.contentagent.common.audit.AuditLogService;
import com.arjunsports.contentagent.common.exception.ConflictException;
import com.arjunsports.contentagent.common.exception.ResourceNotFoundException;
import com.arjunsports.contentagent.modules.user.dto.CreateUserRequest;
import com.arjunsports.contentagent.modules.user.dto.UpdateUserRequest;
import com.arjunsports.contentagent.modules.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public UserResponse create(CreateUserRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("A user with this email already exists");
        }

        User user = User.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .phone(request.phone())
                .password(passwordEncoder.encode(request.password()))
                .role(request.role())
                .status(UserStatus.ACTIVE)
                .active(true)
                .build();

        User saved = userRepository.save(user);
        auditLogService.record(ActivityAction.CREATE, "User", saved.getId(), Map.of("email", saved.getEmail()));
        return UserResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse get(UUID id) {
        return UserResponse.from(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> list(Pageable pageable) {
        return userRepository.findAll(pageable).map(UserResponse::from);
    }

    @Override
    @Transactional
    public UserResponse update(UUID id, UpdateUserRequest request) {
        User user = findOrThrow(id);
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setPhone(request.phone());
        user.setRole(request.role());
        user.setStatus(request.status());
        user.setActive(request.active());

        User saved = userRepository.save(user);
        auditLogService.record(ActivityAction.UPDATE, "User", saved.getId());
        return UserResponse.from(saved);
    }

    private User findOrThrow(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("User", id));
    }
}
