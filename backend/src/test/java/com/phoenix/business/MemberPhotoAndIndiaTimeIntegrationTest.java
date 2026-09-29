package com.phoenix.business;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phoenix.business.domain.*;
import com.phoenix.business.repository.*;
import com.phoenix.business.service.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import javax.imageio.ImageIO;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:photos-india;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE")
@AutoConfigureMockMvc @ActiveProfiles("test") @WithMockUser(username="owner",roles="OWNER")
class MemberPhotoAndIndiaTimeIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired MemberRepository members;
    @Autowired PaymentRepository payments;
    @Autowired MembershipPlanRepository plans;
    @Autowired UserRepository users;
    @Autowired BusinessSettingsRepository settings;
    @Autowired BusinessTimeService time;
    @Autowired ReportService reports;
    @Autowired JdbcTemplate jdbc;

    @org.junit.jupiter.api.BeforeEach void clean(){payments.deleteAll();members.deleteAll();plans.deleteAll();jdbc.update("DELETE FROM audit_logs");}

    @Test void croppedPhotoRoundTripsAndOmittedPhotoIsPreserved() throws Exception {
        var bytes=new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(512,512,BufferedImage.TYPE_INT_RGB),"png",bytes);
        String photo="data:image/png;base64,"+Base64.getEncoder().encodeToString(bytes.toByteArray());
        Map<String,Object> input=new HashMap<>(Map.of("memberCode","PHOTO-"+UUID.randomUUID().toString().substring(0,8),
                "name","Photo Member","joinDate",time.today().toString(),"photoData",photo));
        var response=mvc.perform(post("/api/members").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(input)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.photoData").value(photo)).andReturn();
        long id=json.readTree(response.getResponse().getContentAsString()).get("id").asLong();
        assertEquals(photo,members.findById(id).orElseThrow().getPhotoData());
        mvc.perform(get("/api/members/"+id+"/profile")).andExpect(jsonPath("$.member.photoData").value(photo));
        input.remove("photoData");
        mvc.perform(put("/api/members/"+id).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(input)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.photoData").value(photo));
        input.put("photoData","data:image/png;base64,bm90LWFuLWltYWdl");
        mvc.perform(put("/api/members/"+id).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(input)))
                .andExpect(status().isUnprocessableEntity());
        assertEquals(photo,members.findById(id).orElseThrow().getPhotoData());
        input.put("photoData","");
        mvc.perform(put("/api/members/"+id).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(input)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.photoData").isEmpty());
    }

    @Test void indiaDayIncludesOpeningMidnightAndExcludesNextMidnightInLedgerReportsAndAudit() throws Exception {
        var business=settings.findById(1L).orElseGet(()->{var b=new BusinessSettings();b.setGymName("Phoenix Test");return b;});business.setTimeZone("America/New_York");settings.save(business);
        LocalDate day=LocalDate.of(2026,9,16);
        assertEquals(ZoneId.of("Asia/Kolkata"),time.zone());
        assertEquals(Instant.parse("2026-09-15T18:30:00Z"),time.startOfDay(day));
        Member member=member();
        payment(member,time.startOfDay(day).minusSeconds(1));
        payment(member,time.startOfDay(day));
        payment(member,time.startOfDay(day.plusDays(1)));
        mvc.perform(get("/api/payments").param("from",day.toString()).param("to",day.toString()).param("memberId",member.getId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        try(var workbook=new XSSFWorkbook(new ByteArrayInputStream(reports.payments(day,day,null)))) {
            assertEquals(4,workbook.getSheetAt(0).getLastRowNum());
            assertTrue(workbook.getSheetAt(0).getRow(4).getCell(3).getStringCellValue().contains("16 Sep 2026, 12:00 AM IST"));
        }
        try(var workbook=new XSSFWorkbook(new ByteArrayInputStream(reports.revenue(day,day,null)))) {
            assertEquals("2026-09-16",workbook.getSheetAt(0).getRow(4).getCell(0).getStringCellValue());
            assertEquals("10.00",workbook.getSheetAt(0).getRow(4).getCell(1).getStringCellValue());
        }
        jdbc.update("INSERT INTO audit_logs(action,actor_username,created_at) VALUES ('BOUNDARY','owner',?)",java.sql.Timestamp.from(time.startOfDay(day)));
        jdbc.update("INSERT INTO audit_logs(action,actor_username,created_at) VALUES ('BOUNDARY','owner',?)",java.sql.Timestamp.from(time.startOfDay(day.plusDays(1))));
        mvc.perform(get("/api/audit-logs").param("action","BOUNDARY").param("from",day.toString()).param("to",day.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test void renewalOnExpiryDayStartsTomorrowAndSearchFindsByNameAndCode() throws Exception {
        Member member=member();member.setExpiryDate(time.today());members.save(member);
        MembershipPlan plan=new MembershipPlan();plan.setName("Photo test plan");plan.setDurationMonths(1);plan.setPrice(BigDecimal.TEN);plan.setActive(true);plans.save(plan);
        mvc.perform(post("/api/payments/renew").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(Map.of("memberId",member.getId(),"planId",plan.getId(),"method","CASH"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.periodStart").value(time.today().plusDays(1).toString()));
        mvc.perform(get("/api/members").param("q",member.getMemberCode())).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/members").param("q",member.getName().toLowerCase(Locale.ROOT))).andExpect(jsonPath("$.totalElements").value(1));
    }

    private Member member(){Member m=new Member();m.setMemberCode(UUID.randomUUID().toString().substring(0,8));m.setName("Member "+m.getMemberCode());m.setJoinDate(time.today());return members.save(m);}
    private void payment(Member m,Instant when){Payment p=new Payment();p.setMember(m);p.setReceiptNumber(UUID.randomUUID().toString());p.setPlanNameSnapshot("Monthly");p.setPeriodStart(time.today());p.setPeriodEnd(time.today().plusMonths(1));p.setPaymentDate(when);p.setAmount(BigDecimal.TEN);p.setMethod(Payment.Method.CASH);p.setReceivedBy(users.findByUsernameIgnoreCase("owner").orElseThrow());payments.save(p);}
}
