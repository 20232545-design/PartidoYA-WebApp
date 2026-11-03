package com.partidoya.platform.users.controllers;

import com.partidoya.platform.users.models.FullName;
import com.partidoya.platform.users.models.UserId;
import com.partidoya.platform.users.dto.AdminUserDetailResource;
import com.partidoya.platform.users.dto.AdminUserPageResource;
import com.partidoya.platform.users.dto.UpdateFullNameResource;
import com.partidoya.platform.users.dto.UserAdministrativeActionRequestResource;
import com.partidoya.platform.users.dto.UserResource;
import com.partidoya.platform.users.dto.ChangePasswordResource;
import com.partidoya.platform.users.dto.AdminUserResourceFromEntityAssembler;
import com.partidoya.platform.users.dto.UserResourceFromEntityAssembler;
import com.partidoya.platform.users.services.UserAdministrationService;
import com.partidoya.platform.users.services.UserService;
import com.partidoya.platform.auth.security.CurrentUserService;
import com.partidoya.platform.users.models.Role;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/api/v1/users", produces = APPLICATION_JSON_VALUE)
@Tag(name = "Users", description = "User query and update endpoints")
public class UsersController {
    private final UserService userService;
    private final UserAdministrationService administrationService;
    private final CurrentUserService currentUserService;

    public UsersController(UserService userService, UserAdministrationService administrationService,
                           CurrentUserService currentUserService) {
        this.userService = userService;
        this.administrationService = administrationService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResource> getUserById(@PathVariable Long userId) {
        var user = userService.findById(currentUserService.requireSelf(userId));
        return user
                .map(UserResourceFromEntityAssembler::toResourceFromEntity)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/admin")
    public ResponseEntity<AdminUserPageResource> searchUsersForAdmin(
            @RequestParam(required = false) Long adminId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "ALL") String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        var result = administrationService.search(currentUserService.requireRole(Role.ADMIN_GENERAL), search, role, page, size);
        return ResponseEntity.ok(AdminUserResourceFromEntityAssembler.toResourceFromEntity(result));
    }

    @GetMapping("/admin/{userId}")
    public ResponseEntity<AdminUserDetailResource> getUserDetailForAdmin(
            @PathVariable Long userId,
            @RequestParam(required = false) Long adminId) {
        var detail = administrationService.findDetail(currentUserService.requireRole(Role.ADMIN_GENERAL), new UserId(userId));
        return ResponseEntity.ok(AdminUserResourceFromEntityAssembler.toResourceFromEntity(detail));
    }

    @PatchMapping("/admin/{userId}/activate")
    public ResponseEntity<UserResource> activateUser(@PathVariable Long userId,
                                                     @RequestBody UserAdministrativeActionRequestResource resource) {
        var user = administrationService.activate(
                currentUserService.requireRole(Role.ADMIN_GENERAL),
                new UserId(userId),
                resource.reason());
        return ResponseEntity.ok(UserResourceFromEntityAssembler.toResourceFromEntity(user));
    }

    @PatchMapping("/admin/{userId}/suspend")
    public ResponseEntity<UserResource> suspendUser(@PathVariable Long userId,
                                                    @RequestBody UserAdministrativeActionRequestResource resource) {
        var user = administrationService.suspend(
                currentUserService.requireRole(Role.ADMIN_GENERAL),
                new UserId(userId),
                resource.reason(),
                resource.suspendedUntil());
        return ResponseEntity.ok(UserResourceFromEntityAssembler.toResourceFromEntity(user));
    }

    @PatchMapping("/admin/{userId}/reactivate")
    public ResponseEntity<UserResource> reactivateUser(@PathVariable Long userId,
                                                       @RequestBody UserAdministrativeActionRequestResource resource) {
        var user = administrationService.reactivate(
                currentUserService.requireRole(Role.ADMIN_GENERAL),
                new UserId(userId),
                resource.reason());
        return ResponseEntity.ok(UserResourceFromEntityAssembler.toResourceFromEntity(user));
    }

    @PatchMapping("/{userId}")
    public ResponseEntity<UserResource> updateFullName(@PathVariable Long userId, @RequestBody UpdateFullNameResource resource) {
        var user = userService.updateFullName(currentUserService.requireSelf(userId), new FullName(resource.fullName()));
        return ResponseEntity.ok(UserResourceFromEntityAssembler.toResourceFromEntity(user));
    }

    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword(@RequestBody ChangePasswordResource resource) {
        userService.changePassword(currentUserService.userId(), resource.currentPassword(), resource.newPassword());
        return ResponseEntity.noContent().build();
    }
}
