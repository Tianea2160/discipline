package org.project.discipline.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "jwt")
class JwtConfig {
    var secret: String = "your-secret-key-should-be-very-long-and-secure-in-production"
    var expiration: Long = 86400000 // 24 hours in milliseconds
} 