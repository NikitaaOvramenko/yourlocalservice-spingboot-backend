package com.nikita_ovramenko.sping_all_purpose_server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.nikita_ovramenko.sping_all_purpose_server.configuration.OpenApiConfig;
import com.nikita_ovramenko.sping_all_purpose_server.configuration.SecurityConfig;
import com.nikita_ovramenko.sping_all_purpose_server.common.filter.JwtFilter;
import com.nikita_ovramenko.sping_all_purpose_server.common.service.JwtService;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.service.AppUserService;
import com.nikita_ovramenko.sping_all_purpose_server.quote.service.QuoteSubmissionService;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.controller.AdminUserController;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.service.AdminUserService;
import com.nikita_ovramenko.sping_all_purpose_server.quote.controller.AdminQuoteController;
import com.nikita_ovramenko.sping_all_purpose_server.quote.service.QuoteAdminService;
import com.nikita_ovramenko.sping_all_purpose_server.quotelineitem.controller.AdminQuoteLineItemController;
import com.nikita_ovramenko.sping_all_purpose_server.quotelineitem.service.QuoteLineItemService;
import com.nikita_ovramenko.sping_all_purpose_server.job.controller.AdminJobController;
import com.nikita_ovramenko.sping_all_purpose_server.job.service.JobService;
import com.nikita_ovramenko.sping_all_purpose_server.joblineitem.controller.AdminJobLineItemController;
import com.nikita_ovramenko.sping_all_purpose_server.joblineitem.service.JobLineItemService;
import com.nikita_ovramenko.sping_all_purpose_server.organization.controller.AdminOrganizationController;
import com.nikita_ovramenko.sping_all_purpose_server.organization.service.OrganizationAdminService;
import com.nikita_ovramenko.sping_all_purpose_server.serviceoffering.controller.AdminServiceOfferingController;
import com.nikita_ovramenko.sping_all_purpose_server.serviceoffering.service.ServiceOfferingAdminService;

@WebMvcTest(controllers = {AdminUserController.class, AdminQuoteController.class, AdminQuoteLineItemController.class, AdminJobController.class, AdminJobLineItemController.class, AdminOrganizationController.class, AdminServiceOfferingController.class},
        properties = {"spring.app.cors_allowed=http://localhost:3000",
                "logging.level.org.springframework.web=INFO"})
@Import({SecurityConfig.class, JwtFilter.class, OpenApiConfig.class})
@ImportAutoConfiguration({
        org.springdoc.core.configuration.SpringDocConfiguration.class,
        org.springdoc.core.properties.SpringDocConfigProperties.class,
        org.springdoc.core.configuration.SpringDocPageableConfiguration.class,
        org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration.class,
        org.springdoc.webmvc.ui.SwaggerConfig.class,
        org.springdoc.core.properties.SwaggerUiConfigProperties.class,
        org.springdoc.core.properties.SwaggerUiOAuthProperties.class})
abstract class AbstractOpenApiTest {
    @Autowired MockMvc mvc;
    @MockitoBean JwtService jwtService;
    @MockitoBean com.nikita_ovramenko.sping_all_purpose_server.app_user.service.AuthSessionService authSessionService;
    @MockitoBean AppUserService appUserService;
    @MockitoBean QuoteSubmissionService submission;
    @MockitoBean AdminUserService adminUserService;
    @MockitoBean QuoteAdminService quoteAdminService;
    @MockitoBean QuoteLineItemService quoteLineItemService;
    @MockitoBean JobService jobService;
    @MockitoBean JobLineItemService jobLineItemService;
    @MockitoBean OrganizationAdminService organizationAdminService;
    @MockitoBean ServiceOfferingAdminService serviceOfferingAdminService;
}
