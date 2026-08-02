package com.coagent.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** 跨域配置：本地开发前端(Vite :5173)直连后端 + 公网同源访问 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 允许任意来源：本地开发(5173) 与 公网部署(cpolar/云服务器等任意域名) 均可访问。
        // 系统无登录鉴权、无 Cookie 凭证，放开来源不引入安全风险；
        // 若后续加鉴权，可收紧为 allowedOrigins 白名单或经 COAGENT_CORS_ORIGIN 注入。
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
