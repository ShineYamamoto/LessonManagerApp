package com.ShineYamamoto.LessonManagerApp.common.aop;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

import lombok.extern.slf4j.Slf4j;

@ControllerAdvice
@Slf4j
public class GlobalControllerAdvice {

	/** データベース関連の例外処理 */
	@ExceptionHandler(DataAccessException.class)
	public String dataAccessExceptionHandler(DataAccessException e, Model model) {
		
		// 空文字をセット
		model.addAttribute("error", "");
		// メッセージをModelに登録
		model.addAttribute("message", "DataAccessExceptionが発生しました");
		// Httpのエラーコード(500)をModelに登録
		model.addAttribute("status", HttpStatus.INTERNAL_SERVER_ERROR);
		
		return "error";
	}
	
	/** 指定されたHTTPステータスでエラー画面を表示する。 */
	@ExceptionHandler(ResponseStatusException.class)
	public String responseStatusExceptionHandler(
			ResponseStatusException e,
			Model model,
			HttpServletResponse response) {
		
		int status = e.getStatusCode().value();
		
		// 実際のHTTPレスポンスにもステータスを設定する
		response.setStatus(status);
		
		model.addAttribute("status", status);
		model.addAttribute("error", "");
		model.addAttribute("message", e.getReason() != null ? e.getReason() : "リクエストを処理できません。");
		
		return "error";
	}

	
	/** その他の例外処理 */
	@ExceptionHandler(Exception.class)
	public String exceptionHandler(Exception e, Model model) {
		
		// 空文字をセット
		model.addAttribute("error", "");
		// メッセージをModelに登録
		model.addAttribute("message", "Exceptionが発生しました");
		// HTTPのエラーコード(500)をModelに登録
		model.addAttribute("status", HttpStatus.INTERNAL_SERVER_ERROR);
		
		return "error";
	}
}
