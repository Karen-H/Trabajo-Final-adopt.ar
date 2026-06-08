import api from './api'

export const proponerReserva = (animalId, adoptanteId) =>
  api.post('/reservas/proponer', { animalId, adoptanteId })

export const aceptarReserva = (reservaId) =>
  api.post(`/reservas/${reservaId}/aceptar`)

export const rechazarReserva = (reservaId) =>
  api.post(`/reservas/${reservaId}/rechazar`)

export const concretarReserva = (reservaId) =>
  api.post(`/reservas/${reservaId}/concretar`)

export const cancelarReserva = (reservaId, motivo) =>
  api.post(`/reservas/${reservaId}/cancelar?motivo=${motivo}`)

export const getReservaPendiente = (rescatistaId) =>
  api.get(`/reservas/pendiente?rescatistaId=${rescatistaId}`)

export const getMisAnimalesDisponibles = (chatId) =>
  api.get(`/reservas/mis-animales-disponibles?chatId=${chatId}`)

export const getMisBloqueos = () =>
  api.get('/reservas/mis-bloqueos')

export const getMisReservasActivas = () =>
  api.get('/reservas/mis-reservas-activas')

export const getMisReservasAdoptante = () =>
  api.get('/reservas/mis-reservas-adoptante')
