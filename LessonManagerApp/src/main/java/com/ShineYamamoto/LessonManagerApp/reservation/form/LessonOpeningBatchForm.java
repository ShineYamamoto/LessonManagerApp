package com.ShineYamamoto.LessonManagerApp.reservation.form;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.Data;

@Data
public class LessonOpeningBatchForm {

	@NotBlank
	private String editId;
	
	@NotBlank
	@Pattern(regexp = "[0-9]{4}-(0[1-9]|1[0-2])")
	private String month;
	
	@NotNull
	@Size(max = 744)
	@Valid
	private List<CreateItem> creates = new ArrayList<>();
	
	@NotNull
	@Size(max = 744)
	@Valid
	private List<UpdateItem> updates = new ArrayList<>();
	
	@NotNull
	@Size(max = 200)
	@Valid
	private List<DeleteItem> deletes = new ArrayList<>();
	
	@Data
	public static class CreateItem {
		
		@NotBlank
		@Size(max = 100)
		private String clientId;
		
		@NotNull
		@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
		private LocalDateTime startsAt;
		
		@NotNull
		@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
		private LocalDateTime endsAt;
	}
	
	@Data
	public static class UpdateItem {
		
		@NotBlank
		@Size(max = 100)
		private String clientId;
		
		@NotNull
		@Positive
		private Long openingId;
		
		@NotNull
		@PositiveOrZero
		private Long expectedVersion;
		
		@NotNull
		@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
		private LocalDateTime startsAt;
		
		@NotNull
		@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
		private LocalDateTime endsAt;
	}
	
	@Data
	public static class DeleteItem {
		
		@NotBlank
		@Size(max = 100)
		private String clientId;
		
		@NotNull
		@Positive
		private Long openingId;
		
		@NotNull
		@PositiveOrZero
		private Long expectedVersion;
	}
}
