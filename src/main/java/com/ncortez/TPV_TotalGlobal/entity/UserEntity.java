package com.ncortez.TPV_TotalGlobal.entity;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ncortez.TPV_TotalGlobal.entity.enums.Role;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

/**
 * Define al usuario del sistema TPV TotalGlobal.
 * Incluye gestión de bloqueos, primer inicio de sesión y roles.
 */
@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private String password;
    private String name;
    private String lastname;
    private String email;

    @Temporal(TemporalType.TIMESTAMP)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "UTC")
    private Date dateCreated;

    @Temporal(TemporalType.TIMESTAMP)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private Date lockedDate;

    @Enumerated(EnumType.STRING)
    private Role role;

    private boolean active;
    private int failedAttempts;
    private boolean firstLogin;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("user")
    private SecurityAnswer securityAnswer;

    /**
     * Constructor vacío obligatorio para JPA
     */
    public UserEntity() {
    }

    /**
     * Constructor completo para creación administrativa
     */
    public UserEntity(String username, String password, String name, String lastname, String email, Role role, boolean active) {
        this.username = username;
        this.password = password;
        this.name = name;
        this.lastname = lastname;
        this.email = email;
        this.role = role;
        this.active = active;
        this.failedAttempts = 0;
        this.firstLogin = (password == null || password.isEmpty());
    }

    /**
     * Constructor para nuevos usuarios (Flujo de invitación)
     */
    public UserEntity(String username, String name, String lastname, String email, Role role) {
        this.username = username;
        this.password = null;
        this.name = name;
        this.lastname = lastname;
        this.email = email;
        this.role = role;
        this.active = true;
        this.failedAttempts = 0;
        this.dateCreated = new Date();
        this.firstLogin = true;
    }

    // GETTERS Y SETTERS
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLastname() { return lastname; }
    public void setLastname(String lastname) { this.lastname = lastname; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Date getDateCreated() { return dateCreated; }
    public void setDateCreated(Date dateCreated) { this.dateCreated = dateCreated; }

    public Date getLockedDate() { return lockedDate; }
    public void setLockedDate(Date lockedDate) { this.lockedDate = lockedDate; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public int getFailedAttempts() { return failedAttempts; }
    public void setFailedAttempts(int failedAttempts) { this.failedAttempts = failedAttempts; }

    public boolean isFirstLogin() { return firstLogin; }
    public void setFirstLogin(boolean firstLogin) { this.firstLogin = firstLogin; }

    public SecurityAnswer getSecurityAnswer() { return securityAnswer; }
    public void setSecurityAnswer(SecurityAnswer securityAnswer) { this.securityAnswer = securityAnswer; }
}