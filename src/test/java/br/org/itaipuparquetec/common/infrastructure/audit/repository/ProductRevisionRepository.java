package br.org.itaipuparquetec.common.infrastructure.audit.repository;

import jakarta.persistence.EntityManagerFactory;

public class ProductRevisionRepository extends AbstractRevisionRepository<Produtct> {

    protected ProductRevisionRepository(EntityManagerFactory entityManagerFactory) {
        super(entityManagerFactory);
    }
}
