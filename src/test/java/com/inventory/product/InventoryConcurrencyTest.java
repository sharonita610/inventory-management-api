package com.inventory.product;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.inventory.common.error.BusinessException;
import com.inventory.common.error.ErrorCode;
import com.inventory.product.service.InventoryService;
import com.inventory.support.IntegrationTest;

class InventoryConcurrencyTest extends IntegrationTest {

	private static final int THREADS = 32;

	@Autowired
	private InventoryService inventoryService;

	@Test
	@DisplayName("등록되지 않은 상품에 동시 입고 100건이 들어와도 상품은 1개만 생기고 수량은 100이다")
	void concurrentInboundOnNewProduct() throws Exception {
		List<Callable<Void>> tasks = new ArrayList<>();
		for (int i = 0; i < 100; i++) {
			tasks.add(() -> {
				inventoryService.inbound("신규 상품", 1);
				return null;
			});
		}

		runConcurrently(tasks);

		assertThat(count("SELECT count(*) FROM product")).isEqualTo(1);
		assertThat(count("SELECT quantity FROM product WHERE name = '신규 상품'")).isEqualTo(100);
		assertThat(count("SELECT count(*) FROM stock_history")).isEqualTo(100);
	}

	@Test
	@DisplayName("재고 50개에 동시 출고 100건이 들어오면 50건만 성공하고 재고는 0이다")
	void concurrentOutboundNeverGoesNegative() throws Exception {
		Long productId = inventoryService.inbound("상품 A", 50).id();
		AtomicInteger succeeded = new AtomicInteger();
		AtomicInteger insufficient = new AtomicInteger();
		List<Callable<Void>> tasks = new ArrayList<>();
		for (int i = 0; i < 100; i++) {
			tasks.add(() -> {
				try {
					inventoryService.outbound(productId, 1);
					succeeded.incrementAndGet();
				} catch (final BusinessException e) {
					if (e.getErrorCode() != ErrorCode.INSUFFICIENT_STOCK) {
						throw e;
					}
					insufficient.incrementAndGet();
				}
				return null;
			});
		}

		runConcurrently(tasks);

		assertThat(succeeded.get()).isEqualTo(50);
		assertThat(insufficient.get()).isEqualTo(50);
		assertThat(inventoryService.getProduct(productId).quantity()).isZero();
		assertThat(count("SELECT count(*) FROM stock_history WHERE type = 'OUTBOUND'")).isEqualTo(50);
	}

	@Test
	@DisplayName("입고와 출고가 동시에 섞여도 최종 수량과 이력이 일치한다")
	void concurrentMixedInboundAndOutbound() throws Exception {
		Long productId = inventoryService.inbound("상품 B", 1000).id();
		List<Callable<Void>> tasks = new ArrayList<>();
		for (int i = 0; i < 100; i++) {
			tasks.add(() -> {
				inventoryService.inbound("상품 B", 3);
				return null;
			});
			tasks.add(() -> {
				inventoryService.outbound(productId, 2);
				return null;
			});
		}

		runConcurrently(tasks);

		assertThat(inventoryService.getProduct(productId).quantity()).isEqualTo(1100);
		assertThat(count("""
				SELECT sum(CASE WHEN type = 'INBOUND' THEN quantity ELSE -quantity END)
				FROM stock_history
				""")).isEqualTo(1100);
		assertThat(count("""
				SELECT quantity_after FROM stock_history ORDER BY id DESC LIMIT 1
				""")).isEqualTo(1100);
	}

	private void runConcurrently(final List<Callable<Void>> tasks) throws Exception {
		ExecutorService executor = Executors.newFixedThreadPool(THREADS);
		CountDownLatch start = new CountDownLatch(1);
		try {
			List<Future<Void>> futures = new ArrayList<>();
			for (Callable<Void> task : tasks) {
				futures.add(executor.submit(() -> {
					start.await();
					return task.call();
				}));
			}
			start.countDown();
			for (Future<Void> future : futures) {
				future.get(30, TimeUnit.SECONDS);
			}
		} finally {
			executor.shutdownNow();
		}
	}

	private long count(final String sql) {
		return Optional.ofNullable(jdbcTemplate.queryForObject(sql, Long.class)).orElse(0L);
	}
}
