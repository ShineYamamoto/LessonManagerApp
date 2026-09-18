package com.ShineYamamoto.LessonManagerApp.goallevel.domain.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ShineYamamoto.LessonManagerApp.goallevel.domain.model.GoalLevel;
import com.ShineYamamoto.LessonManagerApp.goallevel.repository.GoalLevelMapper;

@ExtendWith(MockitoExtension.class)
public class GoalLevelServiceTest {

	/** GoalLevelMapperのモック */
	@Mock
	private GoalLevelMapper mapper;
	
	/** テスト対象 */
	@InjectMocks
	private GoalLevelService goalLevelService;
	
	@Test
	void getSelectableGoalLevelsで選択可能な目標レベル一覧を取得できる () {
		
		// 準備
		GoalLevel n1 = createGoalLevel(1, "N1", 1);
		GoalLevel n2 = createGoalLevel(2, "N2", 2);
		
		List<GoalLevel> expected = List.of(n1, n2);
		
		when(mapper.findAllEnabled()).thenReturn(expected);
		
		// 実行
		List<GoalLevel> actual = goalLevelService.getSelectableGoalLevels();
		
		// 確認
		assertEquals(expected, actual);
		verify(mapper).findAllEnabled();
	}
	
	
	@Test
	void isSelectableで有効なIDの場合はtrueになる () {
		
		// 準備
		Integer goalLevelId = 1;
		
		when(mapper.countEnabledById(goalLevelId))
			.thenReturn(1);
		
		// 実行
		boolean actual = goalLevelService.isSelectable(goalLevelId);
		
		// 確認
		assertTrue(actual);
		verify(mapper).countEnabledById(goalLevelId);
	}
	
	@Test
	void isSelectableで存在しないIDの場合はfalseになる () {
		
		// 準備
		Integer goalLevelId = -99;
		
		when(mapper.countEnabledById(goalLevelId))
			.thenReturn(0);
		
		// 実行
		boolean actual = goalLevelService.isSelectable(goalLevelId);
		
		assertFalse(actual);
		verify(mapper).countEnabledById(goalLevelId);
	}
	
	@Test
	void isSelectableで引数がnullの場合はfalseになりMapperを呼び出さない () {
		
		// 実行
		boolean actual = goalLevelService.isSelectable(null);
		
		// 確認
		assertFalse(actual);
		verifyNoInteractions(mapper);
	}
	
	
	/**
	 * テスト用のGoalLevelを生成
	 */
	private GoalLevel createGoalLevel (
			Integer id,
			String levelCode,
			Integer displayOrder) {
		
		GoalLevel goalLevel = new GoalLevel();
		
		goalLevel.setId(id);
		goalLevel.setLevelCode(levelCode);
		goalLevel.setDisplayOrder(displayOrder);
		goalLevel.setEnabled(true);
		
		return goalLevel;
	}

}
