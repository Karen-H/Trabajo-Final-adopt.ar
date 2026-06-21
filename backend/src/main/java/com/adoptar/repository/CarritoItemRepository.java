package com.adoptar.repository;

import com.adoptar.entity.CarritoItem;
import com.adoptar.entity.ItemTienda;
import com.adoptar.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CarritoItemRepository extends JpaRepository<CarritoItem, Long> {

    List<CarritoItem> findByAdoptante(User adoptante);

    Optional<CarritoItem> findByAdoptanteAndItem(User adoptante, ItemTienda item);

    void deleteByAdoptanteAndItem(User adoptante, ItemTienda item);

    void deleteByAdoptanteAndItemIn(User adoptante, List<ItemTienda> items);
}
