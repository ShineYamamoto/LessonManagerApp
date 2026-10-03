document.addEventListener("DOMContentLoaded", function() {
	const regionCode = document.getElementById("regionCode");
	const phoneNumber = document.getElementById("phoneNumber");

	function updatePhonePlaceholder() {
		
		// 選択されているOptionを見つける
		const selectedOption = regionCode.options[regionCode.selectedIndex];
		
		// placeholderを書き換える
		phoneNumber.placeholder = selectedOption.dataset.placeholder;
	}

	regionCode.addEventListener("change", updatePhonePlaceholder);

	// 初期状態
	updatePhonePlaceholder();		
	
});