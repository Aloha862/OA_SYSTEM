package com.example.oa.security;

import com.example.oa.common.util.SecurityUtils;
import com.example.oa.security.service.LoginUser;
import org.springframework.stereotype.Component;

@Component("securityPermission")
public class SecurityPermission {

    public boolean isApprover() {
        LoginUser user = SecurityUtils.currentUser();
        return user != null && (SecurityUtils.isAdmin() || (user.getIsApprover() != null && user.getIsApprover() == 1));
    }
}
