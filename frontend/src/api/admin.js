import api from './api'

export const eliminarAnimalAdmin = (id, motivo) => api.delete(`/admin/animales/${id}`, { data: { motivo } })
export const eliminarFotoAdmin = (id, motivo) => api.delete(`/admin/fotos/${id}`, { data: { motivo } })

export const getUsuarios = () => api.get('/admin/usuarios')
export const eliminarUsuario = (id) => api.delete(`/admin/usuarios/${id}`)
export const actualizarRol = (id, rol) => api.patch(`/admin/usuarios/${id}/rol`, { rol })

export const getTiendasActivas = () => api.get('/admin/tiendas/activas')
export const revocarTienda = (usuarioId) => api.delete(`/admin/tiendas/${usuarioId}`)

export const getDashboardStats = () => api.get('/admin/dashboard')
