package com.hopesapms.app.modules.auth.dto;

import java.util.List;

import com.hopesapms.app.modules.user.model.User;
import lombok.Getter;
import lombok.Setter;

import java.util.stream.Collectors;
import org.springframework.security.core.GrantedAuthority;

@Getter
@Setter
public class AuthProfileDTO {

    private String firstName;
    private String middleName;
    private String lastName;
    private String username;
    private String email;
    private String phoneNumber;
    private String avatarUrl;
    private String departmentId;

    private List<String> roles;

    public AuthProfileDTO(User user) {
        this.firstName = user.getFirstName();
        this.middleName = user.getMiddleName();
        this.lastName = user.getLastName();
        this.username = user.getAppUsername();
        this.email = user.getEmail();
        this.phoneNumber = user.getPhoneNumber();
        this.avatarUrl = user.getProfilePictureUrl();
        this.roles = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        if (user.getDepartment() != null) {
            this.departmentId = user.getDepartment().getId().toString();
        } else {
            this.departmentId = null;
        }
    }
}