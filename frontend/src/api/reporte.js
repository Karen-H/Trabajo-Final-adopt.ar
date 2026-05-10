import api from './api'

export function crearReporte(formData) {
  return api.post('/reportes', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function getPerdidos() {
  return api.get('/reportes/perdidos')
}

export function getEncontrados() {
  return api.get('/reportes/encontrados')
}

export function getMisReportes() {
  return api.get('/reportes/mis-reportes')
}

export function resolverReporte(id) {
  return api.put(`/reportes/${id}/resolver`)
}

export function buscarNominatim(query) {
  // excluye negocios y POIs, solo calles, barrios, ciudades y direcciones exactas
  const params = new URLSearchParams({
    q: query,
    format: 'json',
    addressdetails: '1',
    limit: '5',
    featuretype: 'settlement,street,house',
  })
  return fetch(
    `https://nominatim.openstreetmap.org/search?${params}`,
    { headers: { 'Accept-Language': 'es' } }
  ).then(r => r.json())
}
