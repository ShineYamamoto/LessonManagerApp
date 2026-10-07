package com.ShineYamamoto.LessonManagerApp.common.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.ShineYamamoto.LessonManagerApp.security.LoginUser;

import lombok.extern.slf4j.Slf4j;

@Aspect
@Component
@Slf4j
public class ControllerLoggingAspect {

	/** GetMappingを対象にする */
	@Pointcut("@annotation(org.springframework.web.bind.annotation.GetMapping)")
	public void getMapping() {}
	
	/** PostMappingを対象にする */
	@Pointcut("@annotation(org.springframework.web.bind.annotation.PostMapping)")
	public void postMapping() {}
	
	/** Controllerの開始・終了をログ出力する */
	@Around("getMapping() || postMapping()")
	public Object logging(ProceedingJoinPoint joinPoint) throws Throwable {
		
		String methodName = joinPoint.getSignature().toShortString();
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		String authClassName = authentication.getPrincipal().getClass().getSimpleName();
		LoginUser loginUser; // ログインユーザーの情報を格納する用
		
		// 開始ログ
		// ログインユーザーによる処理か確認
		if (authentication != null && "LoginUser".equals(authClassName)) {
			loginUser = (LoginUser) authentication.getPrincipal();
			log.info(
					"controller_start, user_name={}, method={}",
					loginUser.getDisplayUserName(),
					methodName
			);
		} else {
			log.info(
					"controller_start, user_name={}, method={}",
					"anonymous",
					methodName
			);
		}
		
		
		try {
			
			// メソッド実行
			Object result = joinPoint.proceed();
			
			// 終了ログ
			if (authentication != null && "LoginUser".equals(authClassName)) {
				loginUser = (LoginUser) authentication.getPrincipal();
				log.info(
						"controller_end, user_name={}, method={}",
						loginUser.getDisplayUserName(),
						methodName
				);
			} else {
				log.info(
						"controller_end, user_name={}, method={}",
						"anonymous",
						methodName
				);
			}
			
			
			// 実行結果を呼び出し元に返却
			return result;
			
		} catch (Throwable ex) {
			
			if (authentication != null && "LoginUser".equals(authClassName)) {
				loginUser = (LoginUser) authentication.getPrincipal();
				log.warn(
						"controller_aborted, user_name={}, method={}, exception={}",
						loginUser.getDisplayUserName(),
						methodName,
						ex.getClass().getSimpleName()
				);
			} else {
				log.warn(
						"controller_aborted, user_name={}, method={}, exception={}",
						"anonymous",
						methodName,
						ex.getClass().getSimpleName()
				);
			}

			// エラーの再スロー
			throw ex;
		}
	}
}
