package com.inventory.common.web;

import java.util.Locale;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PageRequests {

	public static final String PRODUCT_SORT_PATTERN = "(?i)id|name|quantity";
	public static final String HISTORY_SORT_PATTERN = "(?i)id|quantity";
	public static final String DIRECTION_PATTERN = "(?i)asc|desc";

	private static final String TIE_BREAKER = "id";

	public static PageRequest of(final int page, final int size, final String sort, final String direction) {
		String property = sort.toLowerCase(Locale.ROOT);
		Sort primary = Sort.by(Sort.Direction.fromString(direction), property);
		if (TIE_BREAKER.equals(property)) {
			return PageRequest.of(page, size, primary);
		}
		return PageRequest.of(page, size, primary.and(Sort.by(Sort.Direction.ASC, TIE_BREAKER)));
	}
}
