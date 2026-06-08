import api from './api'

export const iniciarChat = (rescatistaId, animalId, animalNombre) =>
  api.post('/chats/iniciar', { rescatistaId, animalId, animalNombre })

export const getMisChats = () =>
  api.get('/chats')

export const getMensajes = (chatId) =>
  api.get(`/chats/${chatId}/mensajes`)

export const enviarMensaje = (chatId, contenido) =>
  api.post(`/chats/${chatId}/mensajes`, { contenido })

export const getNoLeidos = () =>
  api.get('/chats/no-leidos')
