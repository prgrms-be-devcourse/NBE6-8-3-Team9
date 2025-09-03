package com.back.back9.global.webMvc

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebMvcConfig : WebMvcConfigurer {
    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/**")
            .allowedOrigins(
                "https://cdpn.io",
                "http://localhost:8888",
                "https://peuronteuendeu.onrender.com",
                "http://localhost:3000",
                "http://localhost:8000",
                "http://localhost:3001",
                "https://d64t5u28gt0rl.cloudfront.net",
                "http://ec2-43-201-179-64.ap-northeast-2.compute.amazonaws.com",
                "http://43.201.179.64"


            )
            .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true)
            .maxAge(3600)
    }
}