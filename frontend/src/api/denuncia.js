import api from './api'

export const denunciarPublicacion = (animalId, data) => api.post(`/denuncias/${animalId}`, data)
export const getDenunciasPendientes = () => api.get('/denuncias/pendientes')
export const desestimar = (id) => api.patch(`/denuncias/${id}/desestimar`)
export const eliminarPublicacionDenuncia = (id) => api.patch(`/denuncias/${id}/eliminar-publicacion`)
