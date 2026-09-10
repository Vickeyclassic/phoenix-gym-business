package com.phoenix.business;

import com.lowagie.text.pdf.PdfName;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import com.phoenix.business.domain.*;
import com.phoenix.business.repository.*;
import com.phoenix.business.service.*;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReceiptBrandingTest {
    @TempDir Path directory;

    @Test void receiptIncludesConfiguredNameAndLocalImageAndSurvivesMissingLogo() throws Exception {
        ImageIO.write(new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB), "png", directory.resolve("gym.png").toFile());
        BusinessSettings s = new BusinessSettings();
        s.setGymName("Example Client Gym");
        s.setLogoUrl(BrandingAssets.PREFIX + "gym.png");
        Member member = new Member(); member.setName("Test Member"); member.setMemberCode("TEST01");
        User user = new User(); user.setDisplayName("Staff");
        Payment payment = new Payment();
        payment.setMember(member); payment.setReceivedBy(user); payment.setReceiptNumber("TEST-R1");
        payment.setPlanNameSnapshot("Monthly"); payment.setPeriodStart(LocalDate.now());
        payment.setPeriodEnd(LocalDate.now().plusMonths(1)); payment.setAmount(BigDecimal.TEN);
        payment.setMethod(Payment.Method.CASH); payment.setPaymentDate(Instant.now());
        PaymentRepository payments = mock(PaymentRepository.class);
        BusinessSettingsRepository settings = mock(BusinessSettingsRepository.class);
        when(payments.findById(1L)).thenReturn(Optional.of(payment));
        when(settings.findById(1L)).thenReturn(Optional.of(s));
        ReceiptService receipts = new ReceiptService(new BrandingAssets(directory.toString()), payments, settings);
        try (PdfReader reader = new PdfReader(receipts.create(1L))) {
            assertEquals(1, reader.getNumberOfPages());
            assertTrue(new PdfTextExtractor(reader).getTextFromPage(1).contains("Example Client Gym"));
            assertNotNull(reader.getPageN(1).getAsDict(PdfName.RESOURCES).getAsDict(PdfName.XOBJECT));
        }
        s.setLogoUrl(BrandingAssets.PREFIX + "missing.png");
        try (PdfReader reader = new PdfReader(receipts.create(1L))) {
            assertTrue(new PdfTextExtractor(reader).getTextFromPage(1).contains("TEST-R1"));
        }
    }
}
