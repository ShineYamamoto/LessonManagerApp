package com.ShineYamamoto.LessonManagerApp.user.controller;

import java.util.Locale;

import jakarta.servlet.http.HttpServletRequest;

import org.modelmapper.ModelMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ShineYamamoto.LessonManagerApp.common.service.CountryCodeService;
import com.ShineYamamoto.LessonManagerApp.goallevel.domain.service.GoalLevelService;
import com.ShineYamamoto.LessonManagerApp.user.domain.model.User;
import com.ShineYamamoto.LessonManagerApp.user.domain.service.UserService;
import com.ShineYamamoto.LessonManagerApp.user.form.SignupForm;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/user")
@Slf4j
@RequiredArgsConstructor
public class SignupController {
	
	private final CountryCodeService countryCodeService;
	private final UserService userService;
	private final GoalLevelService goalLevelService;
	private final ModelMapper modelMapper;
	
	
	/** ユーザー登録画面を表示 */
	@GetMapping("/signup")
	public String getSignup(Model model,@ModelAttribute SignupForm form, Locale locale) {
		
		// 国情報をmodelに格納	
		model.addAttribute(
				"countryList",
				countryCodeService.getCountryList(locale)
		);
		
		// 目標レベルをmodelに格納
		model.addAttribute(
				"goalLevels", 
				goalLevelService.getSelectableGoalLevels()
		);
		
		
		// ユーザー登録画面に画面遷移
		return "user/signup";
	}
	
	/** ユーザー登録処理 */
	@PostMapping("/signup")
	public String postSignup(
			Model model,
			@ModelAttribute @Validated SignupForm form,
			BindingResult bindingResult,
			Locale locale) {
		
		// 入力チェック結果
		if (bindingResult.hasErrors()) {
			// NG：ユーザー登録画面に戻る
			return getSignup(model, form, locale);
		}
		
		
		// formをUserクラスに変換
		User user = modelMapper.map(form, User.class);
		
		// ユーザー登録
		try {
			
			userService.signup(
					user,
					form.getRegionCode(),
					form.getPhoneNumber());
			
		} catch (IllegalArgumentException e) {
			
			bindingResult.rejectValue(
					"phoneNumber",
					"phoneNumber.invalid"
			);
			
			return getSignup(model, form, locale);
		}
		
		
		// ログイン画面にリダイレクト
		return "redirect:/login";
	}
	
	
	/** ユーザーID重複の例外処理 */
	@ExceptionHandler(DuplicateKeyException.class)
	public String duplicateExceptionHandler (
			DuplicateKeyException e, 
			Model model,
			HttpServletRequest request,
			RedirectAttributes redirectAttributes) {
		
		// 入力内容の取得
		SignupForm form = generateFormFormRequest(request);
		redirectAttributes.addFlashAttribute("signupForm", form);
		
		// エラーメッセージ
		String errorMessage = "このユーザーIDは既に使用されています";
		redirectAttributes.addFlashAttribute("errorMessage", errorMessage);
		
		return "redirect:/user/signup";
	}
	
	/** その他の例外処理 */
	@ExceptionHandler(Exception.class)
	public String allExceptionHandler(
			Exception e,
			Model model,
			HttpServletRequest request,
			RedirectAttributes redirectAttributes) {
		
		// 入力内容の取得
		SignupForm form = generateFormFormRequest(request);
		redirectAttributes.addFlashAttribute("signupForm", form);
		
		// エラーメッセージ
		String errorMessage = "システムエラーが発生しました";
		redirectAttributes.addFlashAttribute("errorMessage", errorMessage);
		
		return "redirect:/user/signup";
	}
	
	/** リクエストからSignupFormを生成する */
	private SignupForm generateFormFormRequest(HttpServletRequest request) {
		
		// リクエストの値をFormにセットする
		SignupForm form = new SignupForm();
		form.setRegionCode(request.getParameter("regionCode"));
		form.setPhoneNumber(request.getParameter("phoneNumber"));
		form.setUserName(request.getParameter("userName"));
		form.setGoalLevelId(
				Integer.valueOf(request.getParameter("goalLevelId")));
		
		return form;
	}
}
