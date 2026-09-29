package com.phoenix.business.web;

import com.phoenix.business.domain.BusinessSettings;
import com.phoenix.business.repository.BusinessSettingsRepository;
import com.phoenix.business.service.AuditService;
import com.phoenix.business.service.BrandingAssets;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.io.IOException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController @RequestMapping("/api/settings") @RequiredArgsConstructor
public class SettingsController {
 private final BusinessSettingsRepository repo; private final AuditService audit; private final BrandingAssets brandingAssets;
 record Input(@NotBlank @Size(max=160)String gymName,@Size(max=30)String phone,@Email @Size(max=160)String email,@Size(max=500)String address,@NotBlank @Pattern(regexp="^[A-Z]{3}$")String currency,@NotBlank @Size(max=60)String timeZone,@Size(max=500)String logoUrl,@Size(max=500)String receiptFooter,@Size(max=80)String taxIdentifier,@Min(0)@Max(365)int reminderDays,boolean allowMultipleDailyCheckins){}
 @GetMapping public BusinessSettings get(){BusinessSettings s=repo.findById(1L).orElseThrow();s.setTimeZone("Asia/Kolkata");return s;}
 @PutMapping @PreAuthorize("hasAnyRole('OWNER','ADMIN')") public BusinessSettings put(@Valid @RequestBody Input input){BusinessSettings s=get();s.setGymName(input.gymName().trim());s.setPhone(input.phone());s.setEmail(input.email());s.setAddress(input.address());s.setCurrency(input.currency());s.setTimeZone("Asia/Kolkata");s.setLogoUrl(blankToNull(input.logoUrl()));s.setReceiptFooter(input.receiptFooter());s.setTaxIdentifier(input.taxIdentifier());s.setReminderDays(input.reminderDays());s.setAllowMultipleDailyCheckins(input.allowMultipleDailyCheckins());s=repo.save(s);audit.record("BUSINESS_SETTINGS_UPDATED","BUSINESS_SETTINGS",1,"Business settings updated");return s;}
 @PostMapping(value="/logo",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @PreAuthorize("hasAnyRole('OWNER','ADMIN')") public Map<String,String> uploadLogo(@RequestPart("file") MultipartFile file)throws IOException{String logoUrl=brandingAssets.store(file.getBytes());audit.record("BUSINESS_LOGO_UPLOADED","BUSINESS_SETTINGS",1,"Business logo uploaded");return Map.of("logoUrl",logoUrl);}
 private String blankToNull(String value){return value==null||value.isBlank()?null:value.trim();}
}
