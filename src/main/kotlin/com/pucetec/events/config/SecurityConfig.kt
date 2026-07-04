package com.pucetec.events.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain

@Configuration
class SecurityConfig {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        // Configuramos la cadena de filtros de seguridad para nuestra API stateless
        http
            // Deshabilitamos CSRF porque no usamos cookies de sesión
            .csrf { it.disable() }
            // Establecemos política de sesión sin estado (stateless)
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    // Permitimos el acceso público de lectura a la cartelera de eventos
                    .requestMatchers(HttpMethod.GET, "/api/events", "/api/events/**").permitAll()
                    // Cualquier otra petición (creación, reserva, cancelación) requiere un token válido
                    .anyRequest().authenticated()
            }
            // Habilitamos la validación automática de tokens JWT de AWS Cognito
            .oauth2ResourceServer { oauth2 -> oauth2.jwt { } }
            
        return http.build()
    }
}
