// SecurityUtils.java
package com.oau.farmerbuyer.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {
    private SecurityUtils() {}
    public static Long currentUserId() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null) return null;
        Object p = a.getPrincipal();
        if (p instanceof Long l) return l;
        try { return Long.valueOf(String.valueOf(p)); } catch (Exception e) { return null; }
    }
    public static boolean hasRole(String role) {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a != null && a.getAuthorities().stream().anyMatch(ga -> ga.getAuthority().equals("ROLE_"+role));
    }

    public static Long currentUserId(Authentication auth) {
        if (auth == null) throw new IllegalStateException("No authentication in context");
        Object p = auth.getPrincipal();
        if (p instanceof String s) return Long.valueOf(s);
        if (p instanceof org.springframework.security.core.userdetails.User u) {
            return Long.valueOf(u.getUsername());
        }
        if (p instanceof Long l) return l;
        throw new IllegalStateException("Unable to parse principal as userId");
    }


}
