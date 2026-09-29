package com.phoenix.business;
import com.fasterxml.jackson.databind.*;
import com.phoenix.business.domain.*;
import com.phoenix.business.repository.*;
import com.phoenix.business.service.MembershipExpiryService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:client-security;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
 "app.bootstrap-business-name=B.G. Fitness", "app.bootstrap-logo-url=/api/public/branding-assets/bg-fitness.jpg"})
@AutoConfigureMockMvc @ActiveProfiles("test")
class ClientAcceptanceIntegrationTest {
 @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired UserRepository users;
 @Autowired MemberRepository members; @Autowired PasswordEncoder encoder; @Autowired MembershipExpiryService expiry;
 static final String INITIAL="InitialTest123", CHANGED="ChangedTest456";
 @BeforeEach void resetOwner(){User u=users.findByUsernameIgnoreCase("owner").orElseThrow();u.setPasswordHash(encoder.encode(INITIAL));u.setForcePasswordChange(true);users.save(u);}
 String body(Object v)throws Exception{return json.writeValueAsString(v);}
 String login(String username,String password)throws Exception{return json.readTree(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
  .content(body(Map.of("username",username,"password",password)))).andExpect(status().isOk()).andExpect(jsonPath("$.user.passwordHash").doesNotExist()).andReturn().getResponse().getContentAsString()).get("accessToken").asText();}
 void change(String t,String old,String next)throws Exception{mvc.perform(post("/api/auth/change-password").header("Authorization","Bearer "+t).contentType(MediaType.APPLICATION_JSON)
  .content(body(Map.of("currentPassword",old,"newPassword",next)))).andExpect(status().isOk());}
 String owner()throws Exception{String t=login("owner",INITIAL);change(t,INITIAL,CHANGED);return t;}
 @Test void bootstrapGatePasswordChangeLogoutAndRelogin()throws Exception{
  String t=login("owner",INITIAL);
  mvc.perform(get("/api/auth/me").header("Authorization","Bearer "+t)).andExpect(jsonPath("$.forcePasswordChange").value(true));
  for(String p:List.of("dashboard","members","plans","payments","attendance","users","audit-logs","settings","reports/members.xlsx"))
   mvc.perform(get("/api/"+p).header("Authorization","Bearer "+t)).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("PASSWORD_CHANGE_REQUIRED"));
  mvc.perform(post("/api/members").header("Authorization","Bearer "+t).contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
  mvc.perform(get("/api/public/branding")).andExpect(status().isOk()).andExpect(jsonPath("$.gymName").value("B.G. Fitness"));
  change(t,INITIAL,CHANGED);assertFalse(users.findByUsernameIgnoreCase("owner").orElseThrow().isForcePasswordChange());
  assertTrue(encoder.matches(CHANGED,users.findByUsernameIgnoreCase("owner").orElseThrow().getPasswordHash()));
  mvc.perform(get("/api/dashboard").header("Authorization","Bearer "+t)).andExpect(status().isOk()).andExpect(jsonPath("$.totalMembers").isNumber());
  mvc.perform(post("/api/auth/logout").header("Authorization","Bearer "+t)).andExpect(status().isOk());
  mvc.perform(get("/api/dashboard").header("Authorization","Bearer "+t)).andExpect(status().isUnauthorized());
  login("owner",CHANGED);mvc.perform(get("/api/dashboard")).andExpect(status().isUnauthorized());
 }
 @Test void invalidChangesKeepGateAndLogoutWorks()throws Exception{
  String t=login("owner",INITIAL);
  for(Map<String,String> q:List.of(Map.of("currentPassword",INITIAL),Map.of("currentPassword",INITIAL,"newPassword",INITIAL),
   Map.of("currentPassword",INITIAL,"newPassword","weak"),Map.of("currentPassword","Wrong12345","newPassword",CHANGED),Map.of("currentPassword",INITIAL,"newPassword","Aa1"+"界".repeat(24))))
   mvc.perform(post("/api/auth/change-password").header("Authorization","Bearer "+t).contentType(MediaType.APPLICATION_JSON).content(body(q))).andExpect(status().isUnprocessableEntity());
  assertTrue(users.findByUsernameIgnoreCase("owner").orElseThrow().isForcePasswordChange());
  mvc.perform(post("/api/auth/logout").header("Authorization","Bearer "+t)).andExpect(status().isOk());
 }
 @Test void temporaryAdminStaffAndExistingTokenAfterResetAreGated()throws Exception{
  String owner=owner();
  for(String role:List.of("ADMIN","STAFF")){
   String username="temporary-"+UUID.randomUUID().toString().substring(0,8);
   JsonNode u=json.readTree(mvc.perform(post("/api/users").header("Authorization","Bearer "+owner).contentType(MediaType.APPLICATION_JSON)
    .content(body(Map.of("username",username,"displayName","Acceptance "+role,"role",role,"password",INITIAL)))).andExpect(status().isOk())
    .andExpect(jsonPath("$.forcePasswordChange").value(true)).andReturn().getResponse().getContentAsString());
   String t=login(username,INITIAL);mvc.perform(get("/api/dashboard").header("Authorization","Bearer "+t)).andExpect(status().isForbidden());
   change(t,INITIAL,CHANGED);mvc.perform(get("/api/dashboard").header("Authorization","Bearer "+t)).andExpect(status().isOk());
   mvc.perform(get("/api/users").header("Authorization","Bearer "+t)).andExpect(status().isForbidden());
   mvc.perform(get("/api/reports/members.xlsx").header("Authorization","Bearer "+t)).andExpect(role.equals("ADMIN")?status().isOk():status().isForbidden());
   mvc.perform(put("/api/users/"+u.get("id").asLong()).header("Authorization","Bearer "+owner).contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"OWNER\"}")).andExpect(status().isUnprocessableEntity());
   mvc.perform(post("/api/users/"+u.get("id").asLong()+"/reset-password").header("Authorization","Bearer "+owner).contentType(MediaType.APPLICATION_JSON).content(body(Map.of("password",INITIAL)))).andExpect(status().isOk());
   mvc.perform(get("/api/dashboard").header("Authorization","Bearer "+t)).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("PASSWORD_CHANGE_REQUIRED"));
  }
 }
 @Test void clientWorkflowProfileReportsAndAudit()throws Exception{
  String t=owner(),suffix=UUID.randomUUID().toString().substring(0,8);
  Map<String,Object> plan=new HashMap<>(Map.of("name","Acceptance "+suffix,"durationMonths",1,"price",100,"active",true));
  long pid=json.readTree(mvc.perform(post("/api/plans").header("Authorization","Bearer "+t).contentType(MediaType.APPLICATION_JSON).content(body(plan)))
   .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("id").asLong();
  for(boolean active:List.of(false,true)){plan.put("active",active);mvc.perform(put("/api/plans/"+pid).header("Authorization","Bearer "+t).contentType(MediaType.APPLICATION_JSON).content(body(plan))).andExpect(status().isOk()).andExpect(jsonPath("$.active").value(active));}
  Map<String,Object> member=new HashMap<>(Map.of("memberCode",suffix,"name","Acceptance "+suffix,"joinDate",LocalDate.now().toString(),"status","ACTIVE"));
  long mid=json.readTree(mvc.perform(post("/api/members").header("Authorization","Bearer "+t).contentType(MediaType.APPLICATION_JSON).content(body(member))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("id").asLong();
  member.put("name","Updated "+suffix);mvc.perform(put("/api/members/"+mid).header("Authorization","Bearer "+t).contentType(MediaType.APPLICATION_JSON).content(body(member))).andExpect(status().isOk());
  mvc.perform(get("/api/members?q="+suffix+"&page=0&size=1&sort=name,desc").header("Authorization","Bearer "+t)).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
  mvc.perform(post("/api/payments/renew").header("Authorization","Bearer "+t).contentType(MediaType.APPLICATION_JSON).content(body(Map.of("memberId",mid,"planId",pid,"method","CASH","amount",-1)))).andExpect(status().isUnprocessableEntity());
  long pay=json.readTree(mvc.perform(post("/api/payments/renew").header("Authorization","Bearer "+t).contentType(MediaType.APPLICATION_JSON).content(body(Map.of("memberId",mid,"planId",pid,"method","CASH")))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("id").asLong();
  for(String action:List.of("check-in","check-out"))mvc.perform(post("/api/attendance/"+action+"/"+mid).header("Authorization","Bearer "+t)).andExpect(status().isOk());
  String profile=mvc.perform(get("/api/members/"+mid+"/profile").header("Authorization","Bearer "+t)).andExpect(status().isOk()).andExpect(jsonPath("$.payments[0].id").value(pay)).andExpect(jsonPath("$.recentAttendance[0].source").value("MANUAL")).andReturn().getResponse().getContentAsString();
  assertTrue(json.readTree(profile).findValues("passwordHash").isEmpty());assertFalse(profile.contains("hibernateLazyInitializer"));
  mvc.perform(get("/api/payments?memberId="+mid).header("Authorization","Bearer "+t)).andExpect(jsonPath("$.totalElements").value(1));
  mvc.perform(get("/api/attendance?memberId="+mid).header("Authorization","Bearer "+t)).andExpect(jsonPath("$.totalElements").value(1));
  byte[] pdf=mvc.perform(get("/api/payments/"+pay+"/receipt.pdf").header("Authorization","Bearer "+t)).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
  try(var reader=new com.lowagie.text.pdf.PdfReader(pdf)){assertTrue(new com.lowagie.text.pdf.parser.PdfTextExtractor(reader).getTextFromPage(1).contains("B.G. Fitness"));}
  for(String report:List.of("members","payments","attendance","expiring","revenue")){
   byte[] bytes=mvc.perform(get("/api/reports/"+report+".xlsx").header("Authorization","Bearer "+t)).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
   try(var book=new org.apache.poi.xssf.usermodel.XSSFWorkbook(new java.io.ByteArrayInputStream(bytes))){assertEquals("B.G. Fitness",book.getSheetAt(0).getRow(0).getCell(0).getStringCellValue());}
  }
  mvc.perform(get("/api/audit-logs?action=PAYMENT_CREATED").header("Authorization","Bearer "+t)).andExpect(status().isOk()).andExpect(jsonPath("$.content[0].action").value("PAYMENT_CREATED"));
 }
 @Test void expiryPreservesInactive()throws Exception{
  for(Member.Status status:List.of(Member.Status.ACTIVE,Member.Status.INACTIVE)){
   Member m=new Member();m.setMemberCode(UUID.randomUUID().toString().substring(0,8));m.setName("Expiry test");m.setJoinDate(LocalDate.now());m.setExpiryDate(LocalDate.now().minusDays(1));m.setStatus(status);m=members.save(m);
   expiry.expire();assertEquals(status==Member.Status.ACTIVE?Member.Status.EXPIRED:status,members.findById(m.getId()).orElseThrow().getStatus());
  }
 }
}
