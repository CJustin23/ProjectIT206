package vn.ptit.ffa.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Thông tin OpenAPI/Swagger (thẻ BE-11). Hợp đồng API chuẩn vẫn là docs/api.md —
 * commit nào làm lệch giữa 2 nơi phải sửa tài liệu trước khi merge.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ffmsOpenApi() {
        final String scheme = "bearerAuth";
        return new OpenAPI()
            .info(new Info()
                .title("Budglee API")
                .version("v1")
                .description("Đồ án Nhóm 03 — Hệ thống Quản lý thu chi cá nhân và gia đình (Budglee). "
                    + "Mọi endpoint nghiệp vụ thuộc /api/v1 và luôn lọc theo family_id của token (BR-01).")
                .contact(new Contact().name("Nhóm 03 — Nguyễn Trường An"))
                .license(new License().name("MIT")))
            .addSecurityItem(new SecurityRequirement().addList(scheme))
            .components(new Components().addSecuritySchemes(scheme,
                new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")));
    }
}
