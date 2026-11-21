package com.inventory.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TextNormalizerTest {

	@Test
	@DisplayName("앞뒤 공백은 제거하고 연속된 공백은 하나로 줄인다")
	void collapseWhitespace() {
		assertThat(TextNormalizer.collapseWhitespace("  상품   A  ")).isEqualTo("상품 A");
		assertThat(TextNormalizer.collapseWhitespace("상품\t\nA")).isEqualTo("상품 A");
		assertThat(TextNormalizer.collapseWhitespace("상품\u3000A\u00A0")).isEqualTo("상품 A");
		assertThat(TextNormalizer.collapseWhitespace("상품A")).isEqualTo("상품A");
	}

	@Test
	@DisplayName("null은 그대로 null이다")
	void nullStaysNull() {
		assertThat(TextNormalizer.collapseWhitespace(null)).isNull();
	}
}
