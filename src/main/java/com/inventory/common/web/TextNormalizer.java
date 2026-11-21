package com.inventory.common.web;

import java.util.Optional;
import java.util.regex.Pattern;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TextNormalizer {

	private static final Pattern WHITESPACE = Pattern.compile("(?U)\\s+");

	public static String collapseWhitespace(final String text) {
		return Optional.ofNullable(text)
			.map(value -> WHITESPACE.matcher(value).replaceAll(" ").strip())
			.orElse(null);
	}
}
