package com.hopesapms.app.dto;

import com.hopesapms.app.model.User;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthProfileDTO {
    private String fullName;
    private String username;
    private String email;
    private String avatarUrl;

    public AuthProfileDTO(User user) {
        this.fullName = this.buildFullName(user.getFirstName(), user.getMiddleName(), user.getLastName());
        if (this.fullName == null || this.fullName.isEmpty()) {
            this.fullName = user.getUsername(); 
        }
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.avatarUrl = user.getProfilePictureUrl();
    }

    private String buildFullName(String first, String middle, String last) {
        StringBuilder name = new StringBuilder();
        if (first != null && !first.isEmpty()) {
            name.append(first);
        }
        if (middle != null && !middle.isEmpty()) {
            name.append(" ").append(middle);
        }
        if (last != null && !last.isEmpty()) {
            name.append(" ").append(last);
        }
        return name.toString().trim().replaceAll("\\s+", " ");
    }
}