document.addEventListener("DOMContentLoaded", function() {
	const countryCode = document.getElementById("countryCode");
	const phoneNumber = document.getElementById("phoneNumber");

	function updatePhonePlaceholder() {
		
		// 選択されているOptionを見つける
		const selectedOption = countryCode.options[countryCode.selectedIndex];
		
		// placeholderを書き換える
		phoneNumber.placeholder = selectedOption.dataset.placeholder;
	}

	countryCode.addEventListener("change", updatePhonePlaceholder);

	// 初期状態
	updatePhonePlaceholder();		
	
});