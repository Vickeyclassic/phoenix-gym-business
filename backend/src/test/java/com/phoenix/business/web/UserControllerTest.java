package com.phoenix.business.web;

import com.phoenix.business.domain.User;
import com.phoenix.business.repository.UserRepository;
import com.phoenix.business.service.AuditService;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserControllerTest {
 @Test void primaryOwnerCanChangeOnlyOwnUsernameAndDisplayName(){
  UserRepository repo=mock(UserRepository.class); AuditService audit=mock(AuditService.class); Authentication auth=mock(Authentication.class);
  User owner=new User();owner.setId(1L);owner.setUsername("owner");owner.setDisplayName("Owner");owner.setRole(User.Role.OWNER);owner.setActive(true);
  when(repo.findById(1L)).thenReturn(Optional.of(owner));when(repo.findByUsernameIgnoreCase("bg.owner")).thenReturn(Optional.empty());when(repo.save(owner)).thenReturn(owner);when(auth.getName()).thenReturn("owner");
  UserController controller=new UserController(repo,mock(PasswordEncoder.class),audit);
  var view=controller.update(1L,Map.of("username","bg.owner","displayName","B.G. Owner"),auth);
  assertEquals("bg.owner",view.username());assertEquals("B.G. Owner",view.displayName());assertEquals(User.Role.OWNER,owner.getRole());assertTrue(owner.isActive());
  when(auth.getName()).thenReturn("bg.owner");
  assertThrows(IllegalArgumentException.class,()->controller.update(1L,Map.of("role","STAFF"),auth));
  when(auth.getName()).thenReturn("someone-else");assertThrows(AccessDeniedException.class,()->controller.update(1L,Map.of("displayName","Other"),auth));
 }
}
