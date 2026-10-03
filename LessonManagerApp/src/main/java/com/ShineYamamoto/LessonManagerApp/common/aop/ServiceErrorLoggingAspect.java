package com.ShineYamamoto.LessonManagerApp.common.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Aspect
@Component
@Slf4j
public class ServiceErrorLoggingAspect {

	@Pointcut("execution(* com.ShineYamamoto.LessonManagerApp..domain.service..*.*(..))")
	public void domainService() {}
	
	
	@AfterThrowing(value = "domainService()", throwing = "ex")
	public void logError(JoinPoint joinPoint, Exception ex) {
		
		log.warn(
				"service_error method={}, exception={}",
				joinPoint.getSignature().toShortString(),
				ex.getClass().getSimpleName()
		);
	}
	
}
