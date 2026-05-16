import api from './api'

export function getFavoritos() {
  return api.get('/favoritos')
}

export function agregarFavorito(animalId) {
  return api.post(`/favoritos/${animalId}`)
}

export function quitarFavorito(animalId) {
  return api.delete(`/favoritos/${animalId}`)
}
