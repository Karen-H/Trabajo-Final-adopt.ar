import api from './api'

export function getDisponibilidadRescatistaPropia() {
  return api.get('/disponibilidad-rescatista')
}

export function agregarDisponibilidadRescatista(data) {
  return api.post('/disponibilidad-rescatista', data)
}

export function eliminarDisponibilidadRescatista(id) {
  return api.delete(`/disponibilidad-rescatista/${id}`)
}
