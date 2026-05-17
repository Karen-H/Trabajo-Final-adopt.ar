import api from './api'

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

// admin
export const getItemsPendientesAdmin = () =>
  api.get('/admin/items/pendientes')

export const aprobarItemAdmin = (id) =>
  api.put(`/admin/items/${id}/aprobar`)

export const rechazarItemAdmin = (id, motivo) =>
  api.put(`/admin/items/${id}/rechazar`, { motivo })

export const getFotosItemPendientesAdmin = () =>
  api.get('/admin/items/fotos/pendientes')

export const aprobarFotoItemAdmin = (id) =>
  api.put(`/admin/items/fotos/${id}/aprobar`)

export const rechazarFotoItemAdmin = (id, motivo) =>
  api.put(`/admin/items/fotos/${id}/rechazar`, { motivo })
