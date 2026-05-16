import api from './api'

export function crearAnimal(formData) {
  return api.post('/animales', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function getAdopciones() {
  return api.get('/animales')
}

export function getMisAnimales() {
  return api.get('/animales/mis-animales')
}

export function cambiarEstadoAnimal(id, estado) {
  return api.put(`/animales/${id}/estado`, null, { params: { estado } })
}

export function agregarFotosAnimal(id, formData) {
  return api.post(`/animales/${id}/fotos`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function eliminarAnimal(id) {
  return api.delete(`/animales/${id}`)
}

export function republicarAnimal(id) {
  return api.post(`/animales/${id}/republicar`)
}

export function eliminarFotoAnimal(animalId, fotoId) {
  return api.delete(`/animales/${animalId}/fotos/${fotoId}`)
}
