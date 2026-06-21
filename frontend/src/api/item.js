import api from './api'

// catalogo publico
export const getTiendas = (params) =>
  api.get('/items/tiendas', { params })

export const getItemsDeTienda = (rescatistaId) =>
  api.get(`/items/tiendas/${rescatistaId}`)

export const getMisItems = () =>
  api.get('/items/mis-items')

export const crearItem = (formData) =>
  api.post('/items', formData, { headers: { 'Content-Type': 'multipart/form-data' } })

export const editarItem = (id, data) =>
  api.put(`/items/${id}`, data)

export const agregarFotosItem = (id, formData) =>
  api.post(`/items/${id}/fotos`, formData, { headers: { 'Content-Type': 'multipart/form-data' } })

export const eliminarFotoItem = (itemId, fotoId) =>
  api.delete(`/items/${itemId}/fotos/${fotoId}`)

export const eliminarItem = (id) =>
  api.delete(`/items/${id}`)
