import api from './api'

export function getSlots() {
  return api.get('/tienda/slots')
}

export function getMiSolicitud() {
  return api.get('/tienda/solicitud')
}

export function crearSolicitud(data) {
  return api.post('/tienda/solicitud', data)
}

export function editarSolicitud(data) {
  return api.put('/tienda/solicitud', data)
}

export function cancelarSolicitud() {
  return api.delete('/tienda/solicitud')
}

export function reprogramarComoRescatista(data) {
  return api.put('/tienda/solicitud/reprogramar', data)
}

// admin
export function getSolicitudesAdmin() {
  return api.get('/admin/tienda/solicitudes')
}

export function aceptarSolicitud(id, linkLlamada) {
  return api.put(`/admin/tienda/${id}/aceptar`, { linkLlamada })
}

export function editarLinkSolicitud(id, linkLlamada) {
  return api.put(`/admin/tienda/${id}/link`, { linkLlamada })
}

export function aprobarSolicitud(id) {
  return api.put(`/admin/tienda/${id}/aprobar`)
}

export function rechazarSolicitud(id, motivo) {
  return api.put(`/admin/tienda/${id}/rechazar`, { motivo })
}

export function reprogramarSolicitudAdmin(id, motivo) {
  return api.put(`/admin/tienda/${id}/reprogramar`, { motivo })
}

export function getDisponibilidadPropia() {
  return api.get('/admin/disponibilidad')
}

export function agregarDisponibilidad(data) {
  return api.post('/admin/disponibilidad', data)
}

export function eliminarDisponibilidad(id) {
  return api.delete(`/admin/disponibilidad/${id}`)
}
