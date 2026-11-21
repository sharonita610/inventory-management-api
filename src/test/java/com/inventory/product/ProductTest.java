package com.inventory.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.inventory.common.error.BusinessException;
import com.inventory.common.error.ErrorCode;
import com.inventory.product.domain.Product;
import com.inventory.product.domain.StockHistory;
import com.inventory.product.domain.StockMovementType;

class ProductTest {

	@Test
	@DisplayName("새 상품의 수량은 0이다")
	void newProductStartsEmpty() {
		assertThat(Product.of("상품 A").getQuantity()).isZero();
	}

	@Test
	@DisplayName("입고하면 수량이 증가하고 입고 이력을 돌려준다")
	void increase() {
		Product product = productWithStock(1);

		StockHistory history = product.inbound(4);

		assertThat(product.getQuantity()).isEqualTo(5);
		assertThat(history.getType()).isEqualTo(StockMovementType.INBOUND);
		assertThat(history.getQuantity()).isEqualTo(4);
		assertThat(history.getQuantityAfter()).isEqualTo(5);
	}

	@Test
	@DisplayName("출고하면 수량이 감소하고 출고 이력을 돌려준다")
	void decrease() {
		Product product = productWithStock(5);

		StockHistory history = product.outbound(5);

		assertThat(product.getQuantity()).isZero();
		assertThat(history.getType()).isEqualTo(StockMovementType.OUTBOUND);
		assertThat(history.getQuantity()).isEqualTo(5);
		assertThat(history.getQuantityAfter()).isZero();
	}

	@Test
	@DisplayName("재고보다 많이 출고하면 실패하고 수량은 그대로다")
	void decreaseMoreThanStock() {
		Product product = productWithStock(3);

		assertThatThrownBy(() -> product.outbound(4))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.INSUFFICIENT_STOCK);
		assertThat(product.getQuantity()).isEqualTo(3);
	}

	@Test
	@DisplayName("0 이하 수량은 입고와 출고 모두 거부한다")
	void rejectNonPositiveAmount() {
		Product product = productWithStock(3);

		assertThatThrownBy(() -> product.inbound(0)).isInstanceOf(BusinessException.class);
		assertThatThrownBy(() -> product.outbound(-1)).isInstanceOf(BusinessException.class);
	}

	@Test
	@DisplayName("수량 한계를 넘는 입고는 거부한다")
	void rejectOverflow() {
		Product product = productWithStock(Long.MAX_VALUE);

		assertThatThrownBy(() -> product.inbound(1))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.STOCK_LIMIT_EXCEEDED);
	}

	private Product productWithStock(final long quantity) {
		Product product = Product.of("상품 A");
		product.inbound(quantity);
		return product;
	}
}
