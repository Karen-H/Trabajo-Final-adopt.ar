package com.adoptar.repository;

import com.adoptar.entity.ItemFoto;
import com.adoptar.enums.EstadoFoto;
import com.adoptar.enums.EstadoItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ItemFotoRepository extends JpaRepository<ItemFoto, Long> {

    List<ItemFoto> findByEstadoAndItem_EstadoAndItem_EliminadoFalse(EstadoFoto estadoFoto, EstadoItem estadoItem);
}
