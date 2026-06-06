import api from './api'

export function listarRescatistas(params) {
  return api.get('/donaciones/rescatistas', { params })
}

export function configurarDonaciones(data) {
  return api.put('/donaciones/configurar', data)
}

export function crearPreferencia(data) {
  return api.post('/donaciones/preferencia', data)
}

export function getMisDonaciones() {
  return api.get('/donaciones/mis-donaciones')
}

export function confirmarPago(paymentId) {
  return api.get('/donaciones/confirmar', { params: { paymentId } })
}

export function confirmarPorDonacion(donacionId) {
  return api.get(`/donaciones/confirmar-donacion/${donacionId}`)
}
