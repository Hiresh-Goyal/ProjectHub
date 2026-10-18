
package com.projecthub.backend.config;

import com.projecthub.backend.model.Role;
import com.projecthub.backend.repository.RoleRepo;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class RoleInitializer implements CommandLineRunner {

    private final RoleRepo roleRepo;

    public RoleInitializer(RoleRepo roleRepo) {
        this.roleRepo = roleRepo;
    }

    @Override
    public void run(String... args) {
        createRoleIfMissing("ROLE_USER");
        createRoleIfMissing("ROLE_ADMIN");
    }

    private void createRoleIfMissing(String roleName) {
        if (roleRepo.findByName(roleName).isEmpty()) {
            Role role = new Role();
            role.setName(roleName);
            roleRepo.save(role);
        }
    }
}
