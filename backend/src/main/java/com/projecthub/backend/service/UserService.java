package com.projecthub.backend.service;

import com.projecthub.backend.dto.CreateUserRequest;
import com.projecthub.backend.dto.UpdatePasswordRequest;
import com.projecthub.backend.dto.UpdateUserRequest;
import com.projecthub.backend.dto.UserResponse;
import com.projecthub.backend.enums.CompanyRole;
import com.projecthub.backend.exception.DuplicateUserEmailException;
import com.projecthub.backend.exception.UserNotFoundException;
import com.projecthub.backend.mapper.UserMapper;
import com.projecthub.backend.model.*;
import com.projecthub.backend.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class UserService
{
    private final UserRepo userRepo;
    private final UserMapper userMapper;
    private final PasswordEncoder encoder;
    private final RoleRepo roleRepo;
    private final ProjectRepo projectRepo;
    private final ProjectMemberRepo projectMemberRepo;
    private final TaskAssignmentRepo  taskAssignmentRepo;

    public UserService(UserRepo userRepo, UserMapper userMapper, PasswordEncoder encoder, RoleRepo roleRepo, ProjectRepo projectRepo, ProjectMemberRepo projectMemberRepo, TaskAssignmentRepo taskAssignmentRepo) {
        this.userRepo = userRepo;
        this.userMapper = userMapper;
        this.encoder = encoder;
        this.roleRepo = roleRepo;
        this.projectRepo = projectRepo;
        this.projectMemberRepo = projectMemberRepo;
        this.taskAssignmentRepo = taskAssignmentRepo;
    }

    public List<User> getUsers(){
        if(isGlobalAdmin())
            return userRepo.findAll();
        else if(isCompanyOwner()) {
            List<User> employees = userRepo.findAllByCompany(getLoggedInUser().getCompany());
            List<User> noCompany = userRepo.findAllByCompany(null);
            employees.addAll(noCompany);
            return employees;
        }
        User currentUser = getLoggedInUser();
        Company company = currentUser.getCompany();
        if(company == null)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to view all users");

        List<Project> projects = projectRepo.findAllByCompany(company);
        List<Project> ownedProjects = projects.stream()
                .filter(project -> Objects.equals(project.getOwner().getId(), currentUser.getId()))
                .toList();
        if(!ownedProjects.isEmpty())
        {
            return projects.stream()
                    .map(project -> projectMemberRepo.findAllByProjectId(project.getId()))
                    .flatMap(List::stream)
                    .distinct()
                    .map(ProjectMember::getUser)
                    .toList();
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to view all users");
    }

    public User getUserById(long id){

        User currentUser = getLoggedInUser();
        List<ProjectMember> projectMembers = projectMemberRepo.findAllByUserId(id);

        boolean projectOwner = false;
        if(!projectMembers.isEmpty())
            projectOwner = projectMembers.stream()
                    .map(projectMember -> Objects.equals(projectMember.getProject().getOwner().getId(), currentUser.getId()))
                    .toList()
                    .contains(true);


        User user = userRepo.findById(id).orElseThrow(() ->
                new UserNotFoundException("User not found with id : " + id));

        boolean companyOwner = false;
        if(user.getCompany() != null && currentUser.getCompany() != null)
            companyOwner = ((Objects.equals(currentUser.getCompany().getId(), user.getCompany().getId())) && (isCompanyOwner()));


        //your own OR
        // you are global admin OR
        // you are owner of a project which this guy is a part of OR
        // you are owner of company this guy is member in
        if(boolOwnership(id) || isGlobalAdmin() || projectOwner || companyOwner)
            return user;
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
        User user = getLoggedInUser();

        if((isGlobalAdmin() && !Objects.equals(user.getId(), existingUser.getId())) &&
                existingUser.getRoles().stream().noneMatch(role -> role.getName().equals("ROLE_ADMIN"))){

            existingUser.setPassword(encoder.encode(updatePasswordRequest.getNewPass()));
            return;
        }


        if(Objects.equals(getLoggedInUser().getId(), existingUser.getId())) {
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
        else
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"You are not allowed to change this users password");
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
        User user = getUserById(id);
        List<Project> ownedProjects = projectRepo.findAllByOwner(user);

        if(user.getCompanyRole() == CompanyRole.COMPANY_ADMIN)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User is owner of a company");

        if((ownedProjects.isEmpty())) {
            List<ProjectMember> memberOf =projectMemberRepo.findAllByUserId(user.getId());

            //delete all taskAssignment for project member
            memberOf.forEach(projectMember -> taskAssignmentRepo.deleteByProjectMemberId(projectMember.getId()));

            //delete the member project relations for the user
            memberOf.forEach(projectMember -> projectMemberRepo.deleteByProjectIdAndUserId(projectMember.getProject().getId(),id));

            userRepo.deleteById(id);
        }
        else
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot delete user because they own one or more projects"
            );
    }

    public boolean isGlobalAdmin()
    {
        User currentUser = getLoggedInUser();
        return (currentUser.getRoles().stream().anyMatch(role -> role.getName().equals("ROLE_ADMIN")));
    }

    public boolean isCompanyOwner()
    {
        User currentUser = getLoggedInUser();
        return (currentUser.getCompanyRole() == CompanyRole.COMPANY_ADMIN);
    }

    @Transactional
    public void makeAdmin(Long userId)
    {
        User user = getUserById(userId);
        if(user.getRoles().stream().anyMatch(role -> role.getName().equals("ROLE_ADMIN")))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User is already an admin");

        Role admin = roleRepo.findByName("ROLE_ADMIN").orElseThrow(() ->
            new IllegalStateException("The role admin hasn't been initialized")
        );
        user.getRoles().add(admin);
    }



}
