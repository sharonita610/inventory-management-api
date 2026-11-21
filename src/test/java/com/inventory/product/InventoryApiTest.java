package com.inventory.product;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Connection;
import java.sql.Statement;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.inventory.support.IntegrationTest;

class InventoryApiTest extends IntegrationTest {

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private DataSource dataSource;

	private MockMvc mockMvc;

	@BeforeEach
	void setUpMockMvc() {
		mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
	}

	@Test
	@DisplayName("등록되지 않은 상품을 입고하면 상품을 등록하고 입고 처리한다")
	void inboundCreatesProduct() throws Exception {
		inbound("상품 A", 10)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.name").value("상품 A"))
				.andExpect(jsonPath("$.quantity").value(10));
	}

	@Test
	@DisplayName("등록된 상품을 입고하면 수량을 더한다")
	void inboundIncreasesExistingProduct() throws Exception {
		inbound("상품 A", 10);

		inbound("상품 A", 5)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.quantity").value(15));
	}

	@Test
	@DisplayName("공백만 다르게 입력한 상품명은 같은 상품으로 입고한다")
	void inboundNormalizesNameWhitespace() throws Exception {
		inbound("상품 A", 10);

		inbound("  상품    A ", 5)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.name").value("상품 A"))
				.andExpect(jsonPath("$.quantity").value(15));

		mockMvc.perform(get("/api/v1/products").param("name", " 상품  A"))
				.andExpect(jsonPath("$.totalElements").value(1));
	}

	@Test
	@DisplayName("입고 요청 값이 잘못되면 400과 필드별 사유를 반환한다")
	void inboundValidation() throws Exception {
		postJson("/api/v1/inbounds", """
				{"name": " ", "quantity": 0}
				""")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
				.andExpect(jsonPath("$.errors.length()").value(2));
	}

	@Test
	@DisplayName("소수 수량이나 깨진 JSON은 400을 반환한다")
	void malformedBody() throws Exception {
		postJson("/api/v1/inbounds", """
				{"name": "상품 A", "quantity": 1.5}
				""")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

		postJson("/api/v1/inbounds", "{")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
	}

	@Test
	@DisplayName("출고하면 수량이 줄어든다")
	void outbound() throws Exception {
		inbound("상품 A", 10);

		outbound(1, 4)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.quantity").value(6));
	}

	@Test
	@DisplayName("재고보다 많이 출고하면 409를 반환하고 재고는 그대로다")
	void outboundInsufficientStock() throws Exception {
		inbound("상품 A", 3);

		outbound(1, 4)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"));

		mockMvc.perform(get("/api/v1/products/1"))
				.andExpect(jsonPath("$.quantity").value(3));
	}

	@Test
	@DisplayName("저장 가능한 수량 범위를 넘는 입고는 409를 반환하고 재고는 그대로다")
	void inboundExceedsStockLimit() throws Exception {
		inbound("상품 A", Long.MAX_VALUE);

		inbound("상품 A", 1)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("STOCK_LIMIT_EXCEEDED"));

		mockMvc.perform(get("/api/v1/products/1"))
				.andExpect(jsonPath("$.quantity").value(Long.MAX_VALUE));
	}

	@Test
	@DisplayName("다른 트랜잭션이 상품을 잠그고 있으면 대기 시간 초과 후 503을 반환한다")
	void lockTimeout() throws Exception {
		inbound("상품 A", 10);

		try (Connection connection = dataSource.getConnection();
				Statement statement = connection.createStatement()) {
			connection.setAutoCommit(false);
			statement.execute("SELECT id FROM product WHERE id = 1 FOR UPDATE");

			outbound(1, 1)
					.andExpect(status().isServiceUnavailable())
					.andExpect(jsonPath("$.code").value("LOCK_TIMEOUT"));

			connection.rollback();
		}

		mockMvc.perform(get("/api/v1/products/1"))
				.andExpect(jsonPath("$.quantity").value(10));
	}

	@Test
	@DisplayName("없는 상품을 출고하거나 조회하면 404를 반환한다")
	void productNotFound() throws Exception {
		outbound(999, 1)
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));

		mockMvc.perform(get("/api/v1/products/999"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
	}

	@Test
	@DisplayName("출고 수량이 0 이하이거나 상품 ID가 숫자가 아니면 400을 반환한다")
	void outboundValidation() throws Exception {
		outbound(1, 0)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("quantity"));

		postJson("/api/v1/products/abc/outbounds", """
				{"quantity": 1}
				""")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
	}

	@Test
	@DisplayName("상품 ID가 0 이하이면 400을 반환한다")
	void nonPositiveProductId() throws Exception {
		mockMvc.perform(get("/api/v1/products/0"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
				.andExpect(jsonPath("$.errors[0].field").value("productId"));

		outbound(-1, 1)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("productId"));

		mockMvc.perform(get("/api/v1/products/-1/histories"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("productId"));
	}

	@Test
	@DisplayName("페이지 값이 범위를 벗어나면 400을 반환한다")
	void invalidPageParameters() throws Exception {
		mockMvc.perform(get("/api/v1/products").param("page", "-1"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("page"));

		mockMvc.perform(get("/api/v1/products").param("size", "0"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("size"));

		mockMvc.perform(get("/api/v1/products").param("size", "101"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("size"));

		mockMvc.perform(get("/api/v1/products").param("page", "abc"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

		inbound("상품 A", 1);
		mockMvc.perform(get("/api/v1/products/1/histories").param("size", "101"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("size"));
	}

	@Test
	@DisplayName("상품 목록과 재고를 조회한다")
	void getProducts() throws Exception {
		inbound("상품 A", 1);
		inbound("상품 B", 2);

		mockMvc.perform(get("/api/v1/products"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(2))
				.andExpect(jsonPath("$.content[1].name").value("상품 B"));

		mockMvc.perform(get("/api/v1/products").param("name", "상품 B"))
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].quantity").value(2));
	}

	@Test
	@DisplayName("상품 목록을 요청한 기준으로 정렬한다")
	void sortProducts() throws Exception {
		inbound("나", 5);
		inbound("가", 5);
		inbound("다", 9);

		mockMvc.perform(get("/api/v1/products").param("sort", "quantity").param("direction", "desc"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].name").value("다"))
				.andExpect(jsonPath("$.content[1].name").value("나"))
				.andExpect(jsonPath("$.content[2].name").value("가"));

		mockMvc.perform(get("/api/v1/products").param("sort", "NAME"))
				.andExpect(jsonPath("$.content[0].name").value("가"))
				.andExpect(jsonPath("$.content[2].name").value("다"));
	}

	@Test
	@DisplayName("입출고 이력을 오래된 순으로도 조회한다")
	void sortHistories() throws Exception {
		inbound("상품 A", 10);
		outbound(1, 4);

		mockMvc.perform(get("/api/v1/products/1/histories").param("direction", "asc"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].type").value("INBOUND"))
				.andExpect(jsonPath("$.content[1].type").value("OUTBOUND"));
	}

	@Test
	@DisplayName("허용하지 않는 정렬 기준이나 방향은 400을 반환한다")
	void invalidSortParameters() throws Exception {
		mockMvc.perform(get("/api/v1/products").param("sort", "createdAt"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("sort"));

		mockMvc.perform(get("/api/v1/products").param("direction", "up"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("direction"));

		mockMvc.perform(get("/api/v1/products/1/histories").param("sort", "name"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("sort"));
	}

	@Test
	@DisplayName("입출고 이력을 최신순으로 조회한다")
	void getHistories() throws Exception {
		inbound("상품 A", 10);
		outbound(1, 4);

		mockMvc.perform(get("/api/v1/products/1/histories"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(2))
				.andExpect(jsonPath("$.content[0].type").value("OUTBOUND"))
				.andExpect(jsonPath("$.content[0].quantity").value(4))
				.andExpect(jsonPath("$.content[0].quantityAfter").value(6))
				.andExpect(jsonPath("$.content[1].type").value("INBOUND"));
	}

	@Test
	@DisplayName("없는 경로는 공통 에러 형식의 404를 반환한다")
	void unknownPath() throws Exception {
		mockMvc.perform(get("/api/v1/unknown"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("NOT_FOUND"));
	}

	private ResultActions inbound(final String name, final long quantity) throws Exception {
		return postJson("/api/v1/inbounds", """
				{"name": "%s", "quantity": %d}
				""".formatted(name, quantity));
	}

	private ResultActions outbound(final long productId, final long quantity) throws Exception {
		return postJson("/api/v1/products/" + productId + "/outbounds", """
				{"quantity": %d}
				""".formatted(quantity));
	}

	private ResultActions postJson(final String path, final String body) throws Exception {
		return mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body));
	}
}
