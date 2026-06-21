import api from './api'

export function getCarrito() {
  return api.get('/carrito')
}

export function agregarAlCarrito(itemId, cantidad) {
  return api.post('/carrito/items', { itemId, cantidad })
}

export function actualizarCantidadCarrito(itemId, cantidad) {
  return api.put(`/carrito/items/${itemId}`, { cantidad })
}

export function eliminarDelCarrito(itemId) {
  return api.delete(`/carrito/items/${itemId}`)
}
