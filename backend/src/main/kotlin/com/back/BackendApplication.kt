package com.back

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
// JpaAuditing, Scheduling 등 필요한 어노테이션을 여기에 추가하세요.
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
// ⬅️ 2. @ComponentScan이 필요 없습니다.
//    (실행 파일과 모든 코드가 동일한 루트 패키지 아래에 있기 때문입니다)
class BackendApplication {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            runApplication<BackendApplication>(*args)
        }
    }
}