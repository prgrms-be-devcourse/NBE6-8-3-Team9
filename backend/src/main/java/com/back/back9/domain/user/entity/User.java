package com.back.back9.domain.user.entity;

import com.back.back9.domain.wallet.entity.Wallet;
import com.back.back9.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Column(name = "user_login_id", nullable = false, unique = true)
    private String userLoginId;

    @Column(nullable = false, unique = true)
    private String username;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Column(nullable = false)
    private String password;

    @Column(unique = true)
    private String apiKey;

    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Wallet wallet;

    public User() {
    }

    public User(String userLoginId, String username, UserRole role, String password, String apiKey, Wallet wallet) {
        this.userLoginId = userLoginId;
        this.username = username;
        this.role = role;
        this.password = password;
        this.apiKey = apiKey;
        this.wallet = wallet;
    }

    public User(String userLoginId, String username, String password) {
        this.userLoginId = userLoginId;
        this.username = username;
        this.password = password;
        this.role = UserRole.MEMBER;
        this.apiKey = UUID.randomUUID().toString();
    }

    public static UserBuilder builder() {
        return new UserBuilder();
    }

    public String getUserLoginId() {
        return userLoginId;
    }

    public String getUsername() {
        return username;
    }

    public UserRole getRole() {
        return role;
    }

    public String getPassword() {
        return password;
    }

    public String getApiKey() {
        return apiKey;
    }

    public Wallet getWallet() {
        return wallet;
    }

    public void assignId(Long id) {
        super.setId(id);
    }

    public enum UserRole {
        MEMBER, ADMIN
    }

    public void modifyApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }

    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<String> roles = new ArrayList<>();
        if (isAdmin()) {
            roles.add("ROLE_ADMIN");
        } else {
            roles.add("ROLE_MEMBER");
        }

        return roles.stream()
                .map(SimpleGrantedAuthority::new)
                .toList();
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public static class UserBuilder {
        private String userLoginId;
        private String username;
        private UserRole role;
        private String password;
        private String apiKey;
        private Wallet wallet;

        UserBuilder() {
        }

        public UserBuilder userLoginId(String userLoginId) {
            this.userLoginId = userLoginId;
            return this;
        }

        public UserBuilder username(String username) {
            this.username = username;
            return this;
        }

        public UserBuilder role(UserRole role) {
            this.role = role;
            return this;
        }

        public UserBuilder password(String password) {
            this.password = password;
            return this;
        }

        public UserBuilder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public UserBuilder wallet(Wallet wallet) {
            this.wallet = wallet;
            return this;
        }

        public User build() {
            return new User(userLoginId, username, role, password, apiKey, wallet);
        }

        public String toString() {
            return "User.UserBuilder(userLoginId=" + this.userLoginId + ", username=" + this.username + ", role=" + this.role + ", password=" + this.password + ", apiKey=" + this.apiKey + ", wallet=" + this.wallet + ")";
        }
    }
}