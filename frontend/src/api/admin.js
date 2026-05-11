import api from './api'

export const getAnimalesPendientes = () => api.get('/admin/animales/pendientes')
export const aprobarAnimal = (id) => api.put(`/admin/animales/${id}/aprobar`)
export const rechazarAnimal = (id, motivo) => api.put(`/admin/animales/${id}/rechazar`, { motivo })

export const getFotosPendientes = () => api.get('/admin/fotos/pendientes')
export const aprobarFoto = (id) => api.put(`/admin/fotos/${id}/aprobar`)
export const rechazarFoto = (id, motivo) => api.put(`/admin/fotos/${id}/rechazar`, { motivo })

export const getPublicaciones = () => api.get('/admin/animales/publicados')
export const eliminarAnimalAdmin = (id, motivo) => api.delete(`/admin/animales/${id}`, { data: { motivo } })
export const eliminarFotoAdmin = (id, motivo) => api.delete(`/admin/fotos/${id}`, { data: { motivo } })
