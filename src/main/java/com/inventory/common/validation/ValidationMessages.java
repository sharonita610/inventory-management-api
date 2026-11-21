package com.inventory.common.validation;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ValidationMessages {

	public static final String PRODUCT_ID_POSITIVE = "상품 ID는 1 이상이어야 합니다.";
	public static final String PRODUCT_NAME_REQUIRED = "상품명은 필수입니다.";
	public static final String PRODUCT_NAME_TOO_LONG = "상품명은 100자 이하여야 합니다.";
	public static final String QUANTITY_REQUIRED = "수량은 필수입니다.";
	public static final String QUANTITY_POSITIVE = "수량은 1 이상이어야 합니다.";
	public static final String PAGE_RANGE = "page는 0 이상이어야 합니다.";
	public static final String SIZE_RANGE = "size는 1 이상 100 이하여야 합니다.";
	public static final String PRODUCT_SORT_INVALID = "sort는 id, name, quantity 중 하나여야 합니다.";
	public static final String HISTORY_SORT_INVALID = "sort는 id, quantity 중 하나여야 합니다.";
	public static final String DIRECTION_INVALID = "direction은 asc, desc 중 하나여야 합니다.";
}
