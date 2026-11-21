package com.inventory.product.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.inventory.product.domain.StockHistory;

public interface StockHistoryRepository extends JpaRepository<StockHistory, Long> {

	Page<StockHistory> findByProductId(final long productId, final Pageable pageable);
}
