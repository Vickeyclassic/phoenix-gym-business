package com.phoenix.business.web;

import com.phoenix.business.domain.User;
import com.phoenix.business.repository.UserRepository;
import com.phoenix.business.service.AuditService;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/users") @RequiredArgsConstructor @PreAuthorize("hasRole('OWNER')")
public class UserController {
 private static final String USERNAME_PATTERN="^[A-Za-z0-9._-]{3,80}$";
 private final UserRepository repo; private final PasswordEncoder encoder; private final AuditService audit;
 record Input(@NotBlank @Pattern(regexp=USERNAME_PATTERN) String username,@NotBlank @Size(max=120) String displayName,@NotNull User.Role role,@NotBlank @Pattern(regexp="^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{10,72}$") String password,Boolean active){}
 record View(Long id,String username,String displayName,User.Role role,boolean active,boolean forcePasswordChange,java.time.Instant lastLoginAt,java.time.Instant createdAt){}
 private View v(User u){return new View(u.getId(),u.getUsername(),u.getDisplayName(),u.getRole(),u.isActive(),u.isForcePasswordChange(),u.getLastLoginAt(),u.getCreatedAt());}
 @GetMapping public List<View> list(){return repo.findAll().stream().map(this::v).toList();}
 @PostMapping public View create(@Valid @RequestBody Input q){if(q.role()==User.Role.OWNER)throw new IllegalArgumentException("Additional owners cannot be created here");String username=cleanUsername(q.username());if(repo.findByUsernameIgnoreCase(username).isPresent())throw new IllegalArgumentException("Username already exists");User u=new User();u.setUsername(username);u.setDisplayName(cleanDisplayName(q.displayName()));u.setRole(q.role());u.setActive(q.active()==null||q.active());u.setPasswordHash(encoder.encode(q.password()));u=repo.save(u);audit.record("STAFF_CREATED","USER",u.getId(),"Created "+u.getRole()+" account "+u.getUsername());return v(u);}
 @PutMapping("/{id}") @Transactional public View update(@PathVariable Long id,@RequestBody Map<String,Object> q,Authentication authentication){User u=repo.findById(id).orElseThrow();if(u.getRole()==User.Role.OWNER)return updatePrimaryOwner(u,q,authentication);if(q.containsKey("displayName"))u.setDisplayName(cleanDisplayName(String.valueOf(q.get("displayName"))));if(q.containsKey("role")){User.Role role=User.Role.valueOf(String.valueOf(q.get("role")));if(role==User.Role.OWNER)throw new IllegalArgumentException("Additional owners cannot be created here");u.setRole(role);}if(q.containsKey("active"))u.setActive(Boolean.parseBoolean(String.valueOf(q.get("active"))));u=repo.save(u);audit.record("STAFF_UPDATED","USER",id,"Updated "+u.getUsername());return v(u);}
 private View updatePrimaryOwner(User u,Map<String,Object> q,Authentication authentication){if(!u.getUsername().equalsIgnoreCase(authentication.getName()))throw new AccessDeniedException("Only the primary owner can edit their own account");if(q.containsKey("role")||q.containsKey("active"))throw new IllegalArgumentException("Owner role and active status cannot be changed");String username=q.containsKey("username")?cleanUsername(String.valueOf(q.get("username"))):u.getUsername();String displayName=q.containsKey("displayName")?cleanDisplayName(String.valueOf(q.get("displayName"))):u.getDisplayName();repo.findByUsernameIgnoreCase(username).filter(existing->!existing.getId().equals(u.getId())).ifPresent(existing->{throw new IllegalArgumentException("Username already exists");});audit.record("OWNER_PROFILE_UPDATED","USER",u.getId(),"Primary owner username or display name updated");u.setUsername(username);u.setDisplayName(displayName);return v(repo.save(u));}
 private String cleanUsername(String value){String username=value==null?"":value.trim();if(!username.matches(USERNAME_PATTERN))throw new IllegalArgumentException("Username must be 3-80 characters using letters, numbers, dot, underscore or hyphen");return username;}
 private String cleanDisplayName(String value){String displayName=value==null?"":value.trim();if(displayName.isEmpty()||displayName.length()>120)throw new IllegalArgumentException("Display name must be 1-120 characters");return displayName;}
 @PostMapping("/{id}/reset-password") public void reset(@PathVariable Long id,@RequestBody Map<String,String> q){String password=q.get("password");if(password==null||!password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{10,72}$"))throw new IllegalArgumentException("Password does not meet strength requirements");User u=repo.findById(id).orElseThrow();u.setPasswordHash(encoder.encode(password));u.setForcePasswordChange(true);repo.save(u);audit.record("STAFF_PASSWORD_RESET","USER",id,"Password reset required for "+u.getUsername());}
}
