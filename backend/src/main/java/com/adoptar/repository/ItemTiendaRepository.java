package com.adoptar.repository;

import com.adoptar.entity.ItemTienda;
import com.adoptar.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ItemTiendaRepository extends JpaRepository<ItemTienda, Long> {

    List<ItemTienda> findByRescatistaAndEliminadoFalse(User rescatista);

    List<ItemTienda> findAllByRescatista(User rescatista);
}
