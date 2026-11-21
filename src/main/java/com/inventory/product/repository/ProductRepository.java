package com.inventory.product.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.inventory.product.domain.Product;

import jakarta.persistence.LockModeType;

public interface ProductRepository extends JpaRepository<Product, Long> {

	@Query(
		value = "select 1 from (select pg_advisory_xact_lock(hashtextextended(:name, 0))) as acquired",
		nativeQuery = true
	)
	int lockName(@Param("name") final String name);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select p from Product p where p.name = :name")
	Optional<Product> findByNameForUpdate(@Param("name") final String name);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select p from Product p where p.id = :id")
	Optional<Product> findByIdForUpdate(@Param("id") final long id);

	Page<Product> findByName(final String name, final Pageable pageable);
}
