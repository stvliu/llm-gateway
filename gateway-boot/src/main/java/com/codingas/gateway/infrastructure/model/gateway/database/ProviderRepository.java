package com.codingas.gateway.infrastructure.model.gateway.database;

import com.codingas.gateway.domain.model.enums.ProviderState;
import com.codingas.gateway.infrastructure.model.gateway.database.dataobject.ProviderDo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProviderRepository extends JpaRepository<ProviderDo, Long> {
    List<ProviderDo> findByState(ProviderState state);

    default List<ProviderDo> findActive() {
        return findByState(ProviderState.ACTIVE);
    }
}
