package com.gpch.login.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;
import org.springframework.data.annotation.CreatedDate;

import javax.persistence.*;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;

import java.sql.Date;
import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "user_id")
    private int id;
    @Column(name = "email")
    @Email(message = "*Please provide a valid Email")
    @NotEmpty(message = "*Please provide an email")
    private String email;
    public String getEmail() {
        return email;
    } 
    public void setEmail(String email) {
        this.email = email;
    }
    @Column(name = "password")
    @Length(min = 5, message = "*Your password must have at least 5 characters")
    @NotEmpty(message = "*Please provide your password")
    private String password;
    public String getPassword() {
        return password;
    } 
    public void setPassword(String password) {
        this.password = password;
    }
    @Column(name = "name")
    @NotEmpty(message = "*Please provide your name")
    private String name;
    public String getName() {
        return name;
    } 
    public void setName(String name) {
        this.name = name;
    }
    @Column(name = "last_name")
    @NotEmpty(message = "*Please provide your last name")
    private String lastName;
    public String getLastName() {
        return lastName;
    } 
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    @Column(name = "active")
    private int active;
    public int getActive() {
        return active;
    } 
    public void setActive(int active) {
        this.active = active;
    }
    @Column(name = "created_at")
    private Date createdAt;
    public Date getCreatedAt() {
        return createdAt;
    } 
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
    @ManyToMany(cascade = CascadeType.ALL)
    @JoinTable(name = "user_role", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles;    
    public Set<Role> getRoles() {
        return roles;
    } 
    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }
}
