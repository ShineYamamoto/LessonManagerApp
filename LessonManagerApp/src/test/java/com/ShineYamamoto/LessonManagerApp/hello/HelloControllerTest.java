package com.ShineYamamoto.LessonManagerApp.hello;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class HelloControllerTest {
	
	@Test
	void getHelloでHelloが返される() {

		HelloController controller = new HelloController();
		
		String actual = controller.getHello();

		assertEquals("hello", actual);
	}
}
