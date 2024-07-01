package com.example.clocklike_portal.pdf;

import com.example.clocklike_portal.appUser.AppUserEntity;
import com.example.clocklike_portal.pto.PtoDto;
import com.example.clocklike_portal.pto.PtoEntity;
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

    public String generateRegistrationConfirmationForUser() {
        Context context = new Context();
        return templateEngine.process("thymeleaf_template/registration_finished.html", context);
    }

    public String generateNewEmployeeRegisteredMsgForAdmins(AppUserEntity entity) {
        String applier = entity.getFirstName() + " " + entity.getLastName();
        String empId = entity.getAppUserId().toString();
        Context context = new Context();
        context.setVariable("applier", applier);
        context.setVariable("empId", empId);
        return templateEngine.process("thymeleaf_template/new_employee_registrated.html", context);
    }

    public String generateNewTimeOffRequestMsgForAcceptor(PtoDto request) {
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

    public String generateReqConfirmationMsgForApplier() {
        Context context = new Context();
        return templateEngine.process("thymeleaf_template/timeoff_request_confirmed_applier.html", context);
    }

    public String generateReqConfirmationMsgForAcceptor() {
        Context context = new Context();
        return templateEngine.process("thymeleaf_template/timeoff_request_confirmed_acceptor.html", context);
    }

}
