package com.ShineYamamoto.LessonManagerApp.common.aop;

import java.util.concurrent.TimeUnit;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Aspect
@Component
@Order(0)
@Slf4j
public class ServicePerformanceAspect {

	private final long slowThresholdMs;
	
	public ServicePerformanceAspect(
			@Value("${app.performance.slow-threshold-ms:1000}")
			long slowThresholdMs) {
		
		if (slowThresholdMs < 0) {
			throw new IllegalArgumentException(
					"slow-threshold-ms must be >= 0");
		}
		
		this.slowThresholdMs = slowThresholdMs;
	}
	
	@Around(
			"execution(public *"
			+ "com.ShineYamamoto.LessonManagerApp..domain.service..*.*(..))"
	)
	public Object measure(ProceedingJoinPoint joinPoint) throws Throwable {
		
		long start = System.nanoTime();
		String outcome = "returned";
		
		try {
			// 本来のServiceの処理を実行し、戻り値をそのまま返す
			return joinPoint.proceed();
		} catch (Throwable ex) {
			outcome = "threw";
			
			// 元の例外をそのまま呼び出し元へ伝える
			throw ex;
		} finally {
			
			long elapsedMs = TimeUnit.NANOSECONDS.toMillis(
					System.nanoTime() - start);
			
			if (elapsedMs >= slowThresholdMs) {
				log.warn(
						"slow_service method={} elapsedMs={} outcome={}",
						joinPoint.getSignature().toShortString(),
						elapsedMs,
						outcome
				);
			}
		}
	}
	
}
