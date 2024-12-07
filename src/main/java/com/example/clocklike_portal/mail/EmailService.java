package com.example.clocklike_portal.mail;

import com.example.clocklike_portal.appUser.AppUserEntity;
import com.example.clocklike_portal.appUser.AppUserRepository;
import com.example.clocklike_portal.appUser.UserRole;
import com.example.clocklike_portal.appUser.UserRoleRepository;
import com.example.clocklike_portal.pdf.PdfCreator;
import com.example.clocklike_portal.pdf.TemplateGenerator;
import com.example.clocklike_portal.settings.SettingsRepository;
import com.example.clocklike_portal.timeoff.PtoEntity;
import com.example.clocklike_portal.timeoff.TimeOffDto;
import lombok.RequiredArgsConstructor;
import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.EmailAttachment;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.example.clocklike_portal.pdf.PdfCreator.PDF_TEMPLATES;
import static com.example.clocklike_portal.security.SecurityConfig.ADMIN_AUTHORITY;
import static com.example.clocklike_portal.settings.SettingsService.MAILING_ENABLED;

@Component
@RequiredArgsConstructor
public class EmailService {
    ExecutorService executorService = Executors.newFixedThreadPool(5);

    private final SettingsRepository settingsRepository;
    private final UserRoleRepository userRoleRepository;
    private final AppUserRepository appUserRepository;
    private final TemplateGenerator templateGenerator;
    private final PdfCreator pdfCreator;
    private UserRole adminRole;
    @Value("${mail.mailbox.password}")
    private String mailboxPassword;
    private boolean isEnabled = false;

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        this.isEnabled = Boolean.getBoolean(settingsRepository.findBySettingName(MAILING_ENABLED)
                .orElseThrow(() -> new NoSuchElementException("mailingEnabled setting was not found")).getSettingValue());
    }

    public void setEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    public void sendRegistrationConfirmedMsgForUser(AppUserEntity entity) {
        executorService.submit(() -> {
            String subject = "Resjestracja w Portalu Pracownika";
            String msg = templateGenerator.generateRegistrationConfirmationForUser(entity);
            sendMail(subject, msg, entity.getUserEmail());
        });
    }

    public void sendNewEmployeeRegisteredToAdmins(AppUserEntity newEmployee) {
        executorService.submit(() -> {
            getAdminRole();
            List<AppUserEntity> allAdmins = appUserRepository.findAllByUserRolesContaining(adminRole);
            String subject = "Nowy użytkownik " + newEmployee.getFirstName() + " " + newEmployee.getLastName() + " utworzył konto w Portalu Pracownika.";
            String msg = templateGenerator.generateNewEmployeeRegisteredMsgForAdmins(newEmployee);
            allAdmins.forEach(admin -> {
                sendMail(subject, msg, admin.getUserEmail());
            });
        });
    }

    public void sendNewTimeOffRequestMailToAcceptor(TimeOffDto request) {
        executorService.submit(() -> {
            String subject = generateSubject(request);
            String msg = templateGenerator.generateNewTimeOffRequestMsgForAcceptor(request);
            sendMail(subject, msg, request.getAcceptorEmail());
        });
    }

    public void sendTimeOffRequestDeniedMailToApplier(PtoEntity request) {
        executorService.submit(() -> {
            String subject = generateSubject(request);
            String msg = templateGenerator.generateRequestDeniedMsgForApplier(request);
            sendMail(subject, msg, request.getApplier().getUserEmail());
        });
    }

    public void sendTimeOffRequestMailConformation(PtoEntity request, boolean generatePdf) {
        executorService.submit(() -> {
            String subject = generateSubject(request);
            String pdf = null;
            if (generatePdf) {
                pdf = pdfCreator.generateTimeOffRequestPdf(request);
            }
            sendMail(subject, templateGenerator.generateReqConfirmationMsgForApplier(), request.getApplier().getUserEmail(), pdf);
            sendMail(subject, templateGenerator.generateReqConfirmationMsgForAcceptor(), request.getAcceptor().getUserEmail(), pdf);
            if (generatePdf) {
                deleteRequest(pdf);
            }
        });
    }

    private UserRole getAdminRole() {
        if (adminRole == null) {
            this.adminRole = userRoleRepository.findByRoleNameIgnoreCase(ADMIN_AUTHORITY)
                    .orElseThrow(() -> new NoSuchElementException("No supervisor role found"));
        }
        return adminRole;
    }

    private boolean deleteRequest(String fileName) {
        File file = new File(PDF_TEMPLATES + fileName);
        return file.delete();
    }

    private String generateSubject(TimeOffDto dto) {
        String applier = dto.getApplierFirstName() + " " + dto.getApplierLastName();
        String start = dto.getPtoStart().toString().replaceAll("-", ".");
        String end = dto.getPtoEnd().toString().replaceAll("-", ".");
        return "Winosek urlopowy " + applier + " " + start + "-" + end;
    }

    private String generateSubject(PtoEntity request) {
        String applier = request.getApplier().getFirstName() + " " + request.getApplier().getLastName();
        String start = request.getPtoStart().toString().replaceAll("-", ".");
        String end = request.getPtoEnd().toString().replaceAll("-", ".");
        return "Winosek urlopowy " + applier + " " + start + "-" + end;
    }

    private void sendMail(String subject, String msg, String mailTo) {
        this.sendMail(subject, msg, mailTo, null);
    }

    private void sendMail(String subject, String msg, String mailTo, String pdf) {
        if (!isEnabled) return;
        try {
            System.out.println(mailboxPassword);
            HtmlEmail email = new HtmlEmail();
            email.setDebug(true);

            if (pdf != null) {
                EmailAttachment attachment = new EmailAttachment();
                attachment.setPath(PDF_TEMPLATES + pdf);
                attachment.setDisposition(EmailAttachment.ATTACHMENT);
                attachment.setDescription("pdf wniosek");
                attachment.setName("wniosek.pdf");
                email.attach(attachment);
            }

            email.setHostName("smtp.gmail.com");
            email.setSmtpPort(587);
            email.setAuthenticator(new DefaultAuthenticator("biuro@clocklikeminds.com", mailboxPassword));
            email.setStartTLSEnabled(true);
            email.setStartTLSRequired(true);
            email.setFrom("biuro@clocklikeminds.com", "biuro@clocklikeminds.com");
            email.setCharset("UTF-8");

            email.addTo(mailTo, mailTo);
            email.setSubject(subject);
            email.setHtmlMsg(msg);
            email.setTextMsg("Your email client does not support HTML messages");
            email.send();
        } catch (EmailException e) {
            e.printStackTrace();
        }
    }


}
