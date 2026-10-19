package com.projecthub.backend.service;

import com.projecthub.backend.dto.CreateUserRequest;
import com.projecthub.backend.dto.UpdatePasswordRequest;
import com.projecthub.backend.dto.UpdateUserRequest;
import com.projecthub.backend.dto.UserResponse;
import com.projecthub.backend.exception.DuplicateUserEmailException;
import com.projecthub.backend.exception.UserNotFoundException;
import com.projecthub.backend.mapper.UserMapper;
import com.projecthub.backend.model.Project;
import com.projecthub.backend.model.User;
import com.projecthub.backend.repository.ProjectMemberRepo;
import com.projecthub.backend.repository.ProjectRepo;
import com.projecthub.backend.repository.RoleRepo;
import com.projecthub.backend.repository.UserRepo;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
public class UserService
{
    private final UserRepo userRepo;
    private final UserMapper userMapper;
    private final PasswordEncoder encoder;
    private final RoleRepo roleRepo;
    private final ProjectRepo projectRepo;
    private final ProjectMemberRepo projectMemberRepo;

    public UserService(UserRepo userRepo, UserMapper userMapper, PasswordEncoder encoder, RoleRepo roleRepo, ProjectRepo projectRepo, ProjectMemberRepo projectMemberRepo) {
        this.userRepo = userRepo;
        this.userMapper = userMapper;
        this.encoder = encoder;
        this.roleRepo = roleRepo;
        this.projectRepo = projectRepo;
        this.projectMemberRepo = projectMemberRepo;
    }

    public List<User> getUsers(){
        if(isAdmin())
            return userRepo.findAll();
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to view all users");
    }

    public User getUserById(long id){
        if(boolOwnership(id) || isAdmin())
            return userRepo.findById(id).orElseThrow(() ->
                    new UserNotFoundException("User not found with id : " + id)
            );
        else
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to access this user record");
    }

    public UserResponse addUser(CreateUserRequest createUserRequest){
        User user = userMapper.toUser(createUserRequest);
        String email = user.getEmail().trim().toLowerCase(Locale.ROOT);
        user.setEmail(email);
        if(userRepo.existsByEmail(email)){
            throw new DuplicateUserEmailException("Email already exists");
        }
        user.setPassword(encoder.encode(user.getPassword()));
        user.getRoles().add(roleRepo.findByName("ROLE_USER").orElseThrow(() -> new IllegalStateException("The ROLE_USER hasnt been initialized in db")));
        User savedUser = userRepo.save(user);
        return userMapper.toUserResponse(savedUser);
    }

    @Transactional
    public UserResponse updateUser(UpdateUserRequest updateUserRequest, long id){
        User existingUser = getUserById(id);
        updateUserRequest.setEmail(updateUserRequest.getEmail().trim().toLowerCase(Locale.ROOT));

        if (!(existingUser.getEmail().equals(updateUserRequest.getEmail()))
                && (userRepo.existsByEmail(updateUserRequest.getEmail()))) {
            throw new DuplicateUserEmailException("Email already exists");
        }
        userMapper.updateUser(updateUserRequest, existingUser);
        return userMapper.toUserResponse(existingUser);
    }

    public boolean boolOwnership(Long id) {
        User existingUser = userRepo.findById(id).orElseThrow(() ->
                new UserNotFoundException("User not found with id : " + id));
        String email = SecurityContextHolder.getContext().getAuthentication().getName().trim().toLowerCase(Locale.ROOT);
        return email.equals(existingUser.getEmail());
    }

    @Transactional
    public void updatePassword(UpdatePasswordRequest updatePasswordRequest, long id){
        User existingUser = getUserById(id);
        if (!encoder.matches(
                updatePasswordRequest.getCurrentPass(),
                existingUser.getPassword())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Current password is incorrect"
            );
        }

        existingUser.setPassword(encoder.encode(updatePasswordRequest.getNewPass()));
    }

    public User getLoggedInUser()
    {
        String email = SecurityContextHolder.getContext().getAuthentication().getName().trim().toLowerCase(Locale.ROOT);
        return userRepo.findByEmail(email).orElseThrow(() ->
                new UserNotFoundException("User not found with email : " + email)
        );
    }

    @Transactional
    public void  deleteUser(long id){
        User currentUser = getUserById(id);
        List<Project> ownedProjects = projectRepo.findAllByOwner(currentUser);
        if(ownedProjects.isEmpty()) {
            projectMemberRepo.findAllByUserId(currentUser.getId())
                    .forEach(projectMember -> projectMemberRepo.deleteByProjectIdAndUserId(projectMember.getProject().getId(),id));
            userRepo.deleteById(id);
        }
        else
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot delete user because they own one or more projects"
            );
    }

    public boolean isAdmin()
    {
        User currentUser = getLoggedInUser();
        return (currentUser.getRoles().stream().anyMatch(role -> role.getName().equals("ROLE_ADMIN")));
    }

}
