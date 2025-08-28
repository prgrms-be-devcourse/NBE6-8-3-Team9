package com.back

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
// JpaAuditing, Scheduling 등 필요한 어노테이션을 여기에 추가하세요.
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
class BackendApplication {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            runApplication<BackendApplication>(*args)
        }
    }
}