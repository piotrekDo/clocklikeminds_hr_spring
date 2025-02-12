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
import static com.example.clocklike_portal.settings.SettingsService.MAILING_HR_ENABLED;
import static com.example.clocklike_portal.settings.SettingsService.MAILING_LOCAL_ENABLED;

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
    @Value("${mail.hr}")
    private String hrMailbox;
    private boolean isMailingLocalEnabled = false;
    private boolean isMailingHrEnabled = false;

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        this.isMailingLocalEnabled = Boolean.parseBoolean(settingsRepository.findBySettingName(MAILING_LOCAL_ENABLED)
                .orElseThrow(() -> new NoSuchElementException("Setting not found")).getSettingValue());
        this.isMailingHrEnabled = Boolean.parseBoolean(settingsRepository.findBySettingName(MAILING_HR_ENABLED)
                .orElseThrow(() -> new NoSuchElementException("Setting not found")).getSettingValue());
    }

    public void setMailingLocalEnabled(boolean isEnabled) {
        this.isMailingLocalEnabled = isEnabled;
    }

    public void setMailingHrEnabled(boolean isEnabled) {
        this.isMailingHrEnabled = isEnabled;
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

    public void sendTimeOffRequestMailConformation(PtoEntity request, boolean isFreelancer) {
        executorService.submit(() -> {
            String subject = generateSubject(request);
            String pdf = null;
            if (!isFreelancer) {
                pdf = pdfCreator.generateTimeOffRequestPdf(request);
            }
            sendMail(subject, templateGenerator.generateReqConfirmationMsgForApplier(isFreelancer), request.getApplier().getUserEmail(), pdf);
            sendMail(subject, templateGenerator.generateReqConfirmationMsgForAcceptor(isFreelancer), request.getAcceptor().getUserEmail(), pdf);
            if (!isFreelancer && isMailingHrEnabled) {
                sendMail(subject, templateGenerator.generateReqConformationForHr(request), hrMailbox, pdf);
                deleteRequest(pdf);
            }
        });
    }

    public void sendWithdrawConformationForNotResolvedRequest(PtoEntity request) {
        AppUserEntity acceptor = request.getAcceptor();
        AppUserEntity applier = request.getApplier();
        executorService.submit(() -> {
            String subject = generateSubject(request);
            sendMail(subject, templateGenerator.generateTimeOffWithdrawConformationBeforeResolving(), acceptor.getUserEmail());
            sendMail(subject, templateGenerator.generateTimeOffWithdrawConformationBeforeResolving(), applier.getUserEmail());
        });
    }

    public void sendRequestMarkedForWithdrawMessage(PtoEntity request) {
        AppUserEntity acceptor = request.getAcceptor();
        AppUserEntity applier = request.getApplier();
        executorService.submit(() -> {
            String subject = generateSubject(request);
            sendMail(subject, templateGenerator.generateTimeOffRequestMarkedForWithdraw(), acceptor.getUserEmail());
            sendMail(subject, templateGenerator.generateTimeOffRequestMarkedForWithdraw(), applier.getUserEmail());
        });
    }

    public void sendRequestWithdrawDeclinedMessage(PtoEntity request) {
        AppUserEntity acceptor = request.getAcceptor();
        AppUserEntity applier = request.getApplier();
        executorService.submit(() -> {
            String subject = generateSubject(request);
            sendMail(subject, templateGenerator.generateRequestWithdrawDeclined(), acceptor.getUserEmail());
            sendMail(subject, templateGenerator.generateRequestWithdrawDeclined(), applier.getUserEmail());
        });
    }

    public void sendRequestWithdrawnMessage(PtoEntity request) {
        AppUserEntity acceptor = request.getAcceptor();
        AppUserEntity applier = request.getApplier();
        boolean isFreelancer = applier.isFreelancer();
        executorService.submit(() -> {
            String subject = generateSubject(request);
            String pdf = null;
            if (!isFreelancer) {
                pdf = pdfCreator.generateTimeOffRequestPdf(request);
            }
            sendMail(subject, templateGenerator.generateRequestWithdrawConformation(isFreelancer), request.getApplier().getUserEmail(), pdf);
            sendMail(subject, templateGenerator.generateRequestWithdrawConformation(isFreelancer), request.getAcceptor().getUserEmail(), pdf);
            if (!isFreelancer && isMailingHrEnabled) {
                sendMail(subject, templateGenerator.generateRequestWithdrawForHr(request), hrMailbox, pdf);
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
        System.out.println("--------------");
        System.out.println("MAILING LOCAL ENABLED " + isMailingLocalEnabled);
        System.out.println("--------------");

        if (!isMailingLocalEnabled) return;
        System.out.println("SENDING MAIL: " + subject);
        System.out.println("SENDING MAIL: " + mailTo);
        System.out.println("SENDING MAIL: PDF !=null " + (pdf != null));
        System.out.println("--------------");
        try {
            HtmlEmail email = new HtmlEmail();

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
