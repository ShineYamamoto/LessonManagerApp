package com.ShineYamamoto.LessonManagerApp.user.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ShineYamamoto.LessonManagerApp.security.LoginUser;
import com.ShineYamamoto.LessonManagerApp.user.domain.model.User;
import com.ShineYamamoto.LessonManagerApp.user.domain.service.UserService;
import com.ShineYamamoto.LessonManagerApp.user.form.UserNameEditForm;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/user/edit/name")
@RequiredArgsConstructor
public class UserNameEditController {

	private final UserService userService;
	
	@GetMapping
	public String edit(@AuthenticationPrincipal LoginUser loginUser, Model model) {
		User user = userService.getUserById(loginUser.getUserId());
		if (user == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "ユーザーが存在しません。");
		}
		UserNameEditForm form = new UserNameEditForm();
		form.setUserName(user.getUserName());
		model.addAttribute("userNameEditForm", form);
		return "user/edit/name";
	}
	
	@PostMapping
	public String update(
			@Validated @ModelAttribute("userNameEditForm") UserNameEditForm form,
			BindingResult bindingResult,
			@AuthenticationPrincipal LoginUser loginUser,
			HttpServletRequest request,
			HttpServletResponse response,
			RedirectAttributes redirectAttributes) {
		
		if (bindingResult.hasErrors()) {
			return "user/edit/name";
		}
		
		String updatedName = userService.updateUserName(loginUser.getUserId(), form.getUserName());
		refreshLoginDisplayName(loginUser, updatedName, request, response);
		redirectAttributes.addFlashAttribute("successMessage", "ユーザー名を変更しました。");
		return "redirect:/user/detail";
	}
	
	/** spring securityが保持しているログインユーザー情報にも変更を反映する */
	private void refreshLoginDisplayName(
			LoginUser previousPrincipal,
			String newName,
			HttpServletRequest request,
			HttpServletResponse response) {
		
		Authentication before = SecurityContextHolder.getContext().getAuthentication();
		// 承認後にパスワードが消去されていてもLoginUserを再生成できるようにする。
		LoginUser refreshed = new LoginUser(
				previousPrincipal.getUserId(),
				previousPrincipal.getUsername(),
				previousPrincipal.getPassword() == null ? "" : previousPrincipal.getPassword(),
				previousPrincipal.getAuthorities(),
				newName);
		UsernamePasswordAuthenticationToken after =
				UsernamePasswordAuthenticationToken.authenticated(
						refreshed, before.getCredentials(), before.getAuthorities());
		after.setDetails(before.getDetails());
		
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(after);
		SecurityContextHolder.setContext(context);
		
		new HttpSessionSecurityContextRepository().saveContext(context, request, response);
	}
}
