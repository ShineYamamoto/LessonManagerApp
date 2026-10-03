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
public class ControllerErrorLoggingAspect {

	/** GetMappingを対象にする */
	@Pointcut("@annotation(org.springframework.web.bind.annotation.GetMapping)")
	public void getMapping() {}
	
	/** PostMappingを対象にする */
	@Pointcut("@annotation(org.springframework.web.bind.annotation.PostMapping)")
	public void postMapping() {}
	
	@AfterThrowing(value = "getMapping() || postMapping()", throwing="ex")
	public void logError(JoinPoint joinPoint, Throwable ex) {
		
		log.error(
			"controller_error method={}",
			joinPoint.getSignature().toShortString(),
			ex
		);
	}
}
