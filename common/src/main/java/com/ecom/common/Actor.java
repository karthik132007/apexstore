package com.ecom.common;
import java.util.UUID;
import org.springframework.security.core.context.SecurityContextHolder;
public final class Actor {
    private Actor() {}
    public static UUID id() { return UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName()); }
    public static boolean admin() { return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")); }
    public static void owner(UUID owner) { if (!owner.equals(id()) && !admin()) throw ApiException.forbidden(); }
}
