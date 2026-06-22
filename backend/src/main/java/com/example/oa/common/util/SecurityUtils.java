package com.example.oa.common.util;

import com.example.oa.security.service.LoginUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static LoginUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser loginUser)) {
            return null;
        }
        return loginUser;
    }

    public static Long currentUserId() {
        LoginUser user = currentUser();
        return user == null ? null : user.getId();
    }

    public static String currentUsername() {
        LoginUser user = currentUser();
        return user == null ? null : user.getUsername();
    }

    public static boolean isAdmin() {
        LoginUser user = currentUser();
        return user != null && "ADMIN".equals(user.getRole());
    }
}
