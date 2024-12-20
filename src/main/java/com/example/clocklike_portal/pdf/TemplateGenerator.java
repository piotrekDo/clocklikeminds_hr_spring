package com.example.clocklike_portal.pdf;

import com.example.clocklike_portal.appUser.AppUserEntity;
import com.example.clocklike_portal.timeoff.TimeOffDto;
import com.example.clocklike_portal.timeoff.PtoEntity;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

@Component
public class TemplateGenerator {
    private TemplateEngine templateEngine;

    @PostConstruct
    void init() {
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateResolver.setCharacterEncoding("UTF-8");
        templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);
    }

    String parseTimeOffRequestTemplate(PtoEntity request) {
        String applier = request.getApplier().getFirstName() + " " + request.getApplier().getLastName();
        String acceptor = request.getAcceptor().getFirstName() + " " + request.getAcceptor().getLastName();
        String reqDate = request.getRequestDateTime().toLocalDate().toString().replaceAll("-", ".");
        String decisionDate = request.getDecisionDateTime().toLocalDate().toString().replaceAll("-", ".");
        String start = request.getPtoStart().toString().replaceAll("-", ".");
        String end = request.getPtoEnd().toString().replaceAll("-", ".");
        Context context = new Context();
        context.setVariable("applier", applier);
        context.setVariable("days", request.getBusinessDays());
        context.setVariable("type", request.getLeaveType());
        context.setVariable("range", start + " - " + end);
        context.setVariable("applier_sign", reqDate + " " + applier);
        context.setVariable("acceptor_sing", decisionDate + " " + acceptor);
        return templateEngine.process("thymeleaf_template/timeoff_request_pdf_file.html", context);
    }

    public String generateRegistrationConfirmationForUser(AppUserEntity newEmployee) {
        String userFirstName = newEmployee.getFirstName();
        Context context = new Context();
        context.setVariable("name", userFirstName);
        return templateEngine.process("thymeleaf_template/registration_finished.html", context);
    }

    public String generateNewEmployeeRegisteredMsgForAdmins(AppUserEntity newEmployee) {
        String applier = newEmployee.getFirstName() + " " + newEmployee.getLastName();
        String empId = newEmployee.getAppUserId().toString();
        Context context = new Context();
        context.setVariable("applier", applier);
        context.setVariable("empId", empId);
        return templateEngine.process("thymeleaf_template/new_employee_registrated.html", context);
    }

    public String generateNewTimeOffRequestMsgForAcceptor(TimeOffDto request) {
        String applier = request.getApplierFirstName() + " " + request.getApplierLastName();
        Context context = new Context();
        context.setVariable("applier", applier);
        return templateEngine.process("thymeleaf_template/new_timeoff_request.html", context);
    }

    public String generateRequestDeniedMsgForApplier(PtoEntity entity) {
        Context context = new Context();
        context.setVariable("start", entity.getPtoStart().toString().replaceAll("-", "."));
        context.setVariable("end", entity.getPtoEnd().toString().replaceAll("-", "."));
        return templateEngine.process("thymeleaf_template/timeoff_request_denied.html", context);
    }

    public String generateReqConfirmationMsgForApplier(boolean isFreelancer) {
        Context context = new Context();
        context.setVariable("isFreelancer", isFreelancer);
        return templateEngine.process("thymeleaf_template/timeoff_request_confirmed_applier.html", context);
    }

    public String generateReqConfirmationMsgForAcceptor(boolean isFreelancer) {
        Context context = new Context();
        context.setVariable("isFreelancer", isFreelancer);
        return templateEngine.process("thymeleaf_template/timeoff_request_confirmed_acceptor.html", context);
    }

    public String generateReqConformationForHr(PtoEntity entity) {
        Context context = new Context();
        AppUserEntity applierEntity = entity.getApplier();
        String applier = applierEntity.getFirstName() + " " + applierEntity.getLastName();
        context.setVariable("applier", applier);
        return templateEngine.process("thymeleaf_template/timeoff_request_confirmed_hr.html", context);
    }

    public String generateTimeOffWithdrawConformationBeforeResolving() {
        Context context = new Context();
        return templateEngine.process("thymeleaf_template/timeoff_request_canceled_before_resolving.html", context);
    }

    public String generateTimeOffRequestMarkedForWithdraw() {
        Context context = new Context();
        return templateEngine.process("thymeleaf_template/timeoff_request_marked_for_withdraw.html", context);
    }

    public String generateRequestWithdrawDeclined() {
        Context context = new Context();
        return templateEngine.process("thymeleaf_template/timeoff_request_withdraw_declined.html", context);
    }

    public String generateRequestWithdrawConformation(boolean isFreelancer) {
        Context context = new Context();
        context.setVariable("isFreelancer", isFreelancer);
        return templateEngine.process("thymeleaf_template/timeoff_request_withdrawn_conformation.html", context);
    }

    public String generateRequestWithdrawForHr(PtoEntity request) {
        Context context = new Context();
        AppUserEntity applierEntity = request.getApplier();
        String applier = applierEntity.getFirstName() + " " + applierEntity.getLastName();
        context.setVariable("applier", applier);
        return templateEngine.process("thymeleaf_template/timeoff_request_withdraw_hr.html", context);
    }
}
