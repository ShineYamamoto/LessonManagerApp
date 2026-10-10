package com.ShineYamamoto.LessonManagerApp.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.ShineYamamoto.LessonManagerApp.user.form.UserDetailForm;

@SpringBootTest(properties = {
		"spring.sql.init.mode=always",
		"spring.sql.init.schema-locationsclasspath:schema.sql",
		"spring.sql.init.data-locations=optional:classpath:/security-test-no-data.sql",
		"spring.h2.console.enabled=false"
})
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = Replace.ANY)
@Transactional
public class SecurityIntegrationTest {

	private static final long SELF_ID = 101L;
	private static final long OTHER_ID = 102L;
	
	private static final String PASSWORD = "TestPassword123!";
	private static final String SELF_PHONE = "+819012345678";
	
	@Autowired
	private MockMvc mockMvc;
	
	@Autowired
	private JdbcTemplate jdbc;
	
	@Autowired
	private PasswordEncoder passwordEncoder;
	
	//private java.util.function.BooleanSupplier jdkCheck = () -> true;
	
	@BeforeEach
	void setUp() {
		// 外部キーで参照している側から削除する
		jdbc.update("DELETE FROM reservations");
		jdbc.update("DELETE FROM users");
		jdbc.update("DELETE FROM goal_levels");
		
		jdbc.update("""
				INSERT INTO goal_levels
					(id, level_code, display_order, enabled)
				VALUES (1, 'N1', 1, TRUE)
				""");
		
		insertUser(SELF_ID, SELF_PHONE, "本人");
		insertUser(OTHER_ID, "+8190987654321", "他人");
	}
	
	@Test
	void 未ログインでは詳細画面を開けない() throws Exception {
		mockMvc.perform(get("/user/detail"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrlPattern("**/login"))
			.andExpect(unauthenticated());
	}
	
	@ParameterizedTest
	@ValueSource(strings = {
			"/admin/user/list",
			"/admin/user/detail/102"
	})
	void GENERALは管理画面を開けない(String url) throws Exception {
		mockMvc.perform(get(url)
				.with(user(loginUser("ROLE_GENERAL"))))
			.andExpect(status().isForbidden());
	}
	
	@Test
	void ADMINは管理画面を開ける() throws Exception {
		mockMvc.perform(get("/admin/user/list")
				.with(user(loginUser("ROLE_ADMIN"))))
			.andExpect(status().isOk())
			.andExpect(view().name("admin/user/list"));
	}
	
	@Test
	void 他人のIDを送っても本人の詳細が表示される() throws Exception {
		MvcResult result = mockMvc.perform(get("/user/detail")
				.with(user(loginUser("ROLE_GENERAL")))
				.param("userId", String.valueOf(OTHER_ID)))
			.andExpect(status().isOk())
			.andExpect(view().name("user/detail"))
			.andReturn();
		
		assertNotNull(result.getModelAndView());
		
		UserDetailForm form = (UserDetailForm)
				result.getModelAndView()
					.getModel()
					.get("userDetailForm");
		
		assertNotNull(form);
		assertEquals(Long.valueOf(SELF_ID), form.getUserId());
		assertEquals("本人", form.getUserName());
		assertEquals(SELF_PHONE, form.getE164PhoneNumber());
	}
	
	@Test
	void 他人のIDを送っても本人だけ更新される() throws Exception {
		String otherPasswordBefore = passwordOf(OTHER_ID);
		
		mockMvc.perform(post("/user/detail")
				.with(user(loginUser("ROLE_GENERAL")))
				.with(csrf())
				.param("update", "")
				.param("userId", String.valueOf(OTHER_ID))
				.param("userName", "変更後の本人")
				.param("password", "NewPassword123!"))
			.andExpect(status().is3xxRedirection());
		
		// 本人の情報が更新された
		assertEquals("変更後の本人", nameOf(SELF_ID));
		assertTrue(passwordEncoder.matches(
			"NewPassword123!", passwordOf(SELF_ID)));
		
		// 他人の情報は更新されない
		assertEquals("他人", nameOf(OTHER_ID));
		assertEquals(otherPasswordBefore, passwordOf(OTHER_ID));
	}
	
	@Test
	void CSRFトークン無しでは更新できない() throws Exception {
		String passwordBefore = passwordOf(SELF_ID);
		
		mockMvc.perform(post("/user/detail")
				.with(user(loginUser("ROLE_GENERAL")))
				.param("update", "")
				.param("userName", "不正な変更")
				.param("password", "NewPassword123!"))
			.andExpect(status().isForbidden());
		
		assertEquals("本人", nameOf(SELF_ID));
		assertEquals(passwordBefore, passwordOf(SELF_ID));
	}
	
	@Test
	void 不正なSCRFトークンでは更新できない() throws Exception {
		String passwordBefore = passwordOf(SELF_ID);
		
		mockMvc.perform(post("/user/detail")
				.with(user(loginUser("ROLE_GENERAL")))
				.with(csrf().useInvalidToken())
				.param("update", "")
				.param("userName", "不正な変更")
				.param("password", "NewPassword123!"))
			.andExpect(status().isForbidden());
		
		assertEquals("本人", nameOf(SELF_ID));
		assertEquals(passwordBefore, passwordOf(SELF_ID));
	}
	
	@Test
	void 正しい情報でログインでき次のリクエストでも承認が維持される() throws Exception {
		
		MvcResult result = mockMvc.perform(post("/login")
				.with(csrf())
				.param("regionCode", "JP")
				.param("phoneNumber", "09012345678")
				.param("password", PASSWORD))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/home"))
			.andExpect(authenticated().withUsername(SELF_PHONE))
			.andReturn();
				
		MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
		
		assertNotNull(session);
		
		// ここでは.with(user(...))を付けない
		// ログイン時に保存されたセッションだけで認証できるか確認する
		mockMvc.perform(get("/user/detail").session(session))
			.andExpect(status().isOk())
			.andExpect(view().name("user/detail"))
			.andExpect(authenticated().withUsername(SELF_PHONE));
	}
	
	
	// ---------- テスト準備用の共通処理 ----------
	
	private void insertUser(long id, String phone, String name) {
		jdbc.update("""
				INSERT INTO users
					(user_id, e164_phone_number, user_name, password, goal_level_id, role)
				VALUES (?, ?, ?, ?, 1, 'ROLE_GENERAL')
				""",
				id, phone, name, passwordEncoder.encode(PASSWORD));
	}
	
	private LoginUser loginUser(String authority) {
		return new LoginUser(
			SELF_ID,
			SELF_PHONE,
			"unused",
			List.of(new SimpleGrantedAuthority(authority)),
			"本人"
		);
	}
	
	private String nameOf(long userId) {
		return jdbc.queryForObject(
				"SELECT user_name FROM users WHERE user_id = ?",
				String.class,
				userId
		);
	}
	
	private String passwordOf(long userId) {
		return jdbc.queryForObject(
				"SELECT password FROM users WHERE user_id = ?",
				String.class,
				userId
		);
	}
}
