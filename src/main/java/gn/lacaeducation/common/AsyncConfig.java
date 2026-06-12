package gn.lacaeducation.common;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/** Enables {@code @Async} method execution (report-card and exam-convocation PDF generation, etc.). */
@Configuration
@EnableAsync
public class AsyncConfig {
}
