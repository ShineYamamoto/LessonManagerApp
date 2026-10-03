package com.ShineYamamoto.LessonManagerApp.common.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Aspect
@Component
@Slf4j
public class ControllerLoggingAspect {

	/** GetMappingを対象にする */
	@Pointcut("@annotation(org.springframework.web.bind.annotation.GetMapping")
	public void getMapping() {}
	
	/** PostMappingを対象にする */
	@Pointcut("@annotation(org.springframework.web.bind.annotation.PostMapping")
	public void postMapping() {}
	
	/** Controllerの開始・終了をログ出力する */
	@Around("getMapping() || postMapping()")
	public Object logging(ProceedingJoinPoint joinPoint) throws Throwable {
		
		String method = joinPoint.getSignature().toShortString();
		
		// 開始ログ
		log.info(
			"controller_start method={}",
			method
		);
		
		try {
			
			// メソッド実行
			Object result = joinPoint.proceed();
			
			// 終了ログ
			log.info(
				"controller_end method={}",
				method
			);
			
			// 実行結果を呼び出し元に返却
			return result;
			
		} catch (Throwable ex) {
			
			// エラーの再スロー
			throw ex;
		}
	}
}
