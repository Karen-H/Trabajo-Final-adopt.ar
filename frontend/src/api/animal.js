import api from './api'

export function crearAnimal(formData) {
  return api.post('/animales', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function getMisAnimales() {
  return api.get('/animales/mis-animales')
}

export function cambiarEstadoAnimal(id, estado) {
  return api.put(`/animales/${id}/estado`, null, { params: { estado } })
}
