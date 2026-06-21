import api from './api'

export function crearPreferenciaVenta(rescatistaId) {
  return api.post('/ventas/preferencia', { rescatistaId })
}

export function confirmarPagoVenta(paymentId) {
  return api.get('/ventas/confirmar', { params: { paymentId } })
}

export function confirmarPorVenta(ventaId) {
  return api.get(`/ventas/confirmar-venta/${ventaId}`)
}

export function getVentasPendientesDePago() {
  return api.get('/ventas/pendientes-pago')
}

export function marcarVentaEnviada(ventaId) {
  return api.post(`/ventas/${ventaId}/marcar-enviada`)
}

export function getMisVentas() {
  return api.get('/ventas/mis-ventas')
}

export function getMisCompras() {
  return api.get('/ventas/mis-compras')
}

export function getEnvioPendiente(rescatistaId) {
  return api.get(`/ventas/envio-pendiente/${rescatistaId}`)
}

export function elegirMetodoEnvio(ventaId, metodo) {
  return api.post(`/ventas/${ventaId}/envio/metodo`, { metodo })
}

export function volverAElegirMetodoEnvio(ventaId) {
  return api.post(`/ventas/${ventaId}/envio/volver`)
}

export function elegirHorarioRetiro(ventaId, bloqueId) {
  return api.post(`/ventas/${ventaId}/envio/horario`, { bloqueId })
}

export function completarDomicilioEnvio(ventaId, data) {
  return api.post(`/ventas/${ventaId}/envio/domicilio`, data)
}
