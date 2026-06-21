import { useState, useEffect, useRef } from 'react'
import { getMisChats, getMensajes, enviarMensaje } from '../api/chat'
import { proponerReserva, aceptarReserva, rechazarReserva, concretarReserva, cancelarReserva, getReservaPendiente, getMisAnimalesDisponibles, getMisReservasActivas } from '../api/reserva'
import { getEnvioPendiente, elegirMetodoEnvio, elegirHorarioRetiro, completarDomicilioEnvio, volverAElegirMetodoEnvio } from '../api/venta'
import { useAuth } from '../context/AuthContext'

const DIA_LABEL = {
  LUNES: 'Lunes', MARTES: 'Martes', MIERCOLES: 'Miércoles', JUEVES: 'Jueves',
  VIERNES: 'Viernes', SABADO: 'Sábado', DOMINGO: 'Domingo',
}

function Chats() {
  const { user } = useAuth()
  const [chats, setChats] = useState([])
  const [chatActivo, setChatActivo] = useState(null)
  const [chatActivoData, setChatActivoData] = useState(null) // datos del chat activo
  const [mensajes, setMensajes] = useState([])
  const [texto, setTexto] = useState('')
  const [enviando, setEnviando] = useState(false)
  const bottomRef = useRef(null)

  // reservas
  const [animalesDisponibles, setAnimalesDisponibles] = useState([])
  const [animalSeleccionado, setAnimalSeleccionado] = useState('')
  const [mostrarProponer, setMostrarProponer] = useState(false)
  const [reservasPendientes, setReservasPendientes] = useState([]) // adoptante
  const [reservasActivasChat, setReservasActivasChat] = useState([]) // rescatista, reservas activas de este chat
  const [cancelandoReservaId, setCancelAndoReservaId] = useState(null) // ID de reserva que se está cancelando
  const [hoveredMsgId, setHoveredMsgId] = useState(null)

  // bot de envio (comprador)
  const [envioPendiente, setEnvioPendiente] = useState(null)
  const [domicilioForm, setDomicilioForm] = useState({ calle: '', altura: '', piso: '', depto: '', descripcion: '' })
  const [errorEnvio, setErrorEnvio] = useState('')
  const [enviandoEnvio, setEnviandoEnvio] = useState(false)

  // cargar lista de chats
  useEffect(() => {
    cargarChats()
    // polling de lista cada 5 segundos para detectar chats nuevos
    const intervalo = setInterval(cargarChats, 5000)
    return () => clearInterval(intervalo)
  }, [])

  async function cargarChats() {
    try {
      const res = await getMisChats()
      setChats(res.data)
    } catch {
      // sin chats
    }
  }

  // polling de mensajes del chat activo cada 3 segundos
  useEffect(() => {
    if (!chatActivo) return
    cargarMensajes(chatActivo)
    const intervalo = setInterval(() => cargarMensajes(chatActivo), 3000)
    return () => clearInterval(intervalo)
  }, [chatActivo])

  // refrescar reservas del chat cada 10 segundos
  useEffect(() => {
    if (!chatActivo || !chatActivoData) return
    const intervalo = setInterval(() => cargarReservas(chatActivoData), 10000)
    return () => clearInterval(intervalo)
  }, [chatActivo, chatActivoData, user?.activeProfile])

  // refrescar estado del bot de envio cada 10 segundos (solo lado comprador)
  useEffect(() => {
    if (!chatActivo || !chatActivoData) return
    cargarEnvioPendiente(chatActivoData)
    const intervalo = setInterval(() => cargarEnvioPendiente(chatActivoData), 10000)
    return () => clearInterval(intervalo)
  }, [chatActivo, chatActivoData])

  // scroll al último mensaje
  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [mensajes])

  async function cargarMensajes(chatId) {
    try {
      const res = await getMensajes(chatId)
      setMensajes(res.data)
      // actualizar badge: refrescar lista para que baje el contador
      cargarChats()
    } catch {
      // ignorar
    }
  }

  async function handleEnviar(e) {
    e.preventDefault()
    if (!texto.trim() || !chatActivo) return
    setEnviando(true)
    try {
      await enviarMensaje(chatActivo, texto.trim())
      setTexto('')
      cargarMensajes(chatActivo)
    } catch {
      // ignorar
    } finally {
      setEnviando(false)
    }
  }

  // al cambiar de perfil, cerrar el chat abierto
  useEffect(() => {
    setChatActivo(null)
  }, [user?.activeProfile])

  // al cambiar de chat activo, resetear estado y cargar reservas
  useEffect(() => {
    if (!chatActivo) { setChatActivoData(null); setReservasPendientes([]); setReservasActivasChat([]); setEnvioPendiente(null); return }
    setMostrarProponer(false)
    setAnimalSeleccionado('')
    setEnvioPendiente(null)
    setDomicilioForm({ calle: '', altura: '', piso: '', depto: '', descripcion: '' })
    setErrorEnvio('')
  }, [chatActivo])

  // actualizar chatActivoData cuando cambia la lista (sin resetear el panel)
  useEffect(() => {
    if (!chatActivo) return
    const data = chats.find(c => c.id === chatActivo)
    if (data) {
      setChatActivoData(data)
      cargarReservas(data)
    }
  }, [chatActivo, chats])

  async function cargarReservas(data) {
    if (!data?.id || !user) return
    const esRescatista = user.activeProfile === 'RESCATISTA'
    try {
      if (esRescatista) {
        const [animalesRes, reservasRes] = await Promise.all([
          getMisAnimalesDisponibles(data.id),
          getMisReservasActivas(),
        ])
        setAnimalesDisponibles(animalesRes.data)
        setReservasActivasChat(
          reservasRes.data.filter(r => r.adoptanteId === data.otroUsuarioId && r.estado === 'ACTIVA')
        )
      } else {
        if (!data.otroUsuarioId) return
        const res = await getReservaPendiente(data.otroUsuarioId)
        setReservasPendientes(Array.isArray(res.data) ? res.data : [])
      }
    } catch {
      // ignorar
    }
  }

  async function cargarEnvioPendiente(data) {
    if (!data?.id || data.rolEnChat !== 'ADOPTANTE' || !data.otroUsuarioId) { setEnvioPendiente(null); return }
    try {
      const res = await getEnvioPendiente(data.otroUsuarioId)
      setEnvioPendiente(res.status === 204 ? null : res.data)
    } catch {
      setEnvioPendiente(null)
    }
  }

  async function handleElegirMetodoEnvio(metodo) {
    if (!envioPendiente) return
    setErrorEnvio('')
    setEnviandoEnvio(true)
    try {
      const res = await elegirMetodoEnvio(envioPendiente.ventaId, metodo)
      setEnvioPendiente(res.data)
      cargarMensajes(chatActivo)
    } catch (e) {
      setErrorEnvio(e.response?.data || 'No se pudo guardar la opción.')
    } finally {
      setEnviandoEnvio(false)
    }
  }

  async function handleElegirHorarioRetiro(bloqueId) {
    if (!envioPendiente) return
    setErrorEnvio('')
    setEnviandoEnvio(true)
    try {
      const res = await elegirHorarioRetiro(envioPendiente.ventaId, bloqueId)
      setEnvioPendiente(res.data)
      cargarMensajes(chatActivo)
    } catch (e) {
      setErrorEnvio(e.response?.data || 'No se pudo guardar el horario.')
    } finally {
      setEnviandoEnvio(false)
    }
  }

  async function handleVolverAElegirMetodo() {
    if (!envioPendiente) return
    setErrorEnvio('')
    setEnviandoEnvio(true)
    try {
      const res = await volverAElegirMetodoEnvio(envioPendiente.ventaId)
      setEnvioPendiente(res.data)
      cargarMensajes(chatActivo)
    } catch (e) {
      setErrorEnvio(e.response?.data || 'No se pudo volver atrás.')
    } finally {
      setEnviandoEnvio(false)
    }
  }

  async function handleSubmitDomicilio(e) {
    e.preventDefault()
    if (!envioPendiente || !domicilioForm.calle.trim()) { setErrorEnvio('La calle es obligatoria.'); return }
    setErrorEnvio('')
    setEnviandoEnvio(true)
    try {
      const res = await completarDomicilioEnvio(envioPendiente.ventaId, domicilioForm)
      setEnvioPendiente(res.data)
      cargarMensajes(chatActivo)
    } catch (e) {
      setErrorEnvio(e.response?.data || 'No se pudo guardar el domicilio.')
    } finally {
      setEnviandoEnvio(false)
    }
  }

  async function handleProponer() {
    if (!animalSeleccionado || !chatActivoData) return
    try {
      await proponerReserva(Number(animalSeleccionado), chatActivoData.otroUsuarioId)
      setMostrarProponer(false)
      setAnimalSeleccionado('')
      cargarReservas(chatActivoData)
      cargarMensajes(chatActivo)
    } catch (e) {
      alert(e.response?.data || 'No se pudo proponer la reserva.')
    }
  }

  async function handleAceptar(reservaId, animalNombre) {
    if (!window.confirm(
      `¿Querés aceptar la reserva de ${animalNombre}?\n\n` +
      `Al aceptar, el animal quedará reservado exclusivamente para vos y no estará disponible para otras personas. ` +
      `Te comprometés a concretar la adopción. ` +
      `Si la adopción no se concreta por razones que te sean imputables, puede quedar un registro en tu cuenta ` +
      `que limite nuevas reservas durante 1 mes.`
    )) return
    try {
      await aceptarReserva(reservaId)
      cargarReservas(chatActivoData)
      cargarMensajes(chatActivo)
    } catch (e) {
      alert(e.response?.data || 'Error al aceptar.')
    }
  }

  async function handleRechazar(reservaId) {
    try {
      await rechazarReserva(reservaId)
      cargarReservas(chatActivoData)
      cargarMensajes(chatActivo)
    } catch (e) {
      alert(e.response?.data || 'Error al rechazar.')
    }
  }


  async function handleConcretar(reservaId) {
    if (!window.confirm('¿Confirmar que la adopción se concretó?')) return
    try {
      await concretarReserva(reservaId)
      cargarReservas(chatActivoData)
      cargarMensajes(chatActivo)
    } catch (e) {
      alert(e.response?.data || 'Error.')
    }
  }

  async function handleCancelar(reservaId) {
    setCancelAndoReservaId(reservaId)
  }

  async function confirmarCancelacion(reservaId, motivo) {
    setCancelAndoReservaId(null)
    try {
      await cancelarReserva(reservaId, motivo)
      cargarReservas(chatActivoData)
      cargarMensajes(chatActivo)
    } catch (e) {
      alert(e.response?.data || 'Error.')
    }
  }

  function formatHora(iso) {
    const d = new Date(iso)
    return d.toLocaleTimeString('es-AR', { hour: '2-digit', minute: '2-digit' })
  }

  function formatFecha(iso) {
    const d = new Date(iso)
    return d.toLocaleDateString('es-AR', { day: '2-digit', month: '2-digit' })
  }

  return (
    <div style={{ display: 'flex', height: 'calc(100vh - 80px)', maxWidth: 900, margin: '0 auto', border: '1px solid #ccc' }}>
      {/* lista de chats */}
      <div style={{ width: 280, borderRight: '1px solid #ccc', overflowY: 'auto', flexShrink: 0 }}>
        <div style={{ padding: '12px 16px', borderBottom: '1px solid #eee', fontWeight: 600 }}>
          Mis chats
        </div>
        {(() => {
          const chatsFiltrados = chats.filter(c => c.rolEnChat === user?.activeProfile || c.esChatReporte)
          if (chatsFiltrados.length === 0) return (
            <p style={{ padding: 16, color: '#888', fontSize: 14 }}>No tenés chats todavía.</p>
          )
          return chatsFiltrados.map(c => (
          <div
            key={c.id}
            onClick={() => setChatActivo(c.id)}
            style={{
              padding: '12px 16px',
              cursor: 'pointer',
              background: chatActivo === c.id ? '#f0f0f0' : 'transparent',
              borderBottom: '1px solid #eee'
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <strong style={{ fontSize: 14 }}>{c.otroUsuarioNombre}</strong>
              <span style={{ fontSize: 11, color: '#888' }}>{formatFecha(c.ultimoMensajeEn)}</span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 4 }}>
              <span style={{ fontSize: 12, color: '#666', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', maxWidth: 180 }}>
                {c.ultimoMensaje}
              </span>
              {c.noLeidos > 0 && (
                <span style={{ background: '#1a73e8', color: '#fff', borderRadius: 10, fontSize: 11, padding: '1px 7px', flexShrink: 0 }}>
                  {c.noLeidos}
                </span>
              )}
            </div>
          </div>
          ))
        })()}
      </div>

      {/* panel de mensajes */}
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
        {!chatActivo ? (
          <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#888' }}>
            Seleccioná un chat para ver los mensajes.
          </div>
        ) : (
          <>
            {/* mensajes */}
            <div style={{ flex: 1, overflowY: 'auto', padding: 16, display: 'flex', flexDirection: 'column', gap: 8 }}>
              {mensajes.map(m => (
                <div key={m.id} style={{ display: 'flex', flexDirection: 'column', alignItems: m.emisorId == null ? 'center' : m.esPropio ? 'flex-end' : 'flex-start' }}>
                  {m.emisorId == null ? (
                    (() => {
                      const match = m.contenido.match(/^RESERVA:(\d+):(.*)$/)
                      const texto = match ? match[2] : m.contenido
                      const preview = m.animalPreview
                      return (
                        <div
                          style={{ position: 'relative', display: 'inline-block' }}
                          onMouseEnter={() => preview && setHoveredMsgId(m.id)}
                          onMouseLeave={() => setHoveredMsgId(null)}
                        >
                          <span style={{ fontSize: 12, color: '#888', background: '#f0f0f0', padding: '4px 10px', borderRadius: 10, display: 'inline-block', cursor: preview ? 'default' : undefined }}>
                            {texto}
                          </span>
                          {preview && hoveredMsgId === m.id && (
                            <div style={{ position: 'absolute', top: '100%', left: '50%', transform: 'translateX(-50%)', marginTop: 6, background: '#fff', border: '1px solid #ddd', borderRadius: 8, padding: 10, width: 200, boxShadow: '0 2px 8px rgba(0,0,0,0.15)', zIndex: 10, textAlign: 'left' }}>
                              {preview.primeraFotoUrl && (
                                <img src={preview.primeraFotoUrl} alt="animal" style={{ width: '100%', height: 100, objectFit: 'cover', borderRadius: 4, marginBottom: 6 }} />
                              )}
                              <div style={{ fontSize: 13, fontWeight: 600, color: '#222' }}>
                                {preview.nombre || (preview.tipo === 'PERRO' ? 'Perro' : preview.tipo === 'GATO' ? 'Gato' : 'Otro')}
                              </div>
                              <div style={{ fontSize: 11, color: '#666', marginTop: 2 }}>
                                {preview.estado === 'PERDIDO' ? 'Perdido' : preview.estado === 'ENCONTRADO' ? 'Encontrado' : preview.estado === 'EN_ADOPCION' ? 'En adopción' : preview.estado}
                              </div>
                              {preview.descripcion && (
                                <div style={{ fontSize: 11, color: '#888', marginTop: 4, overflow: 'hidden', display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical' }}>
                                  {preview.descripcion}
                                </div>
                              )}
                            </div>
                          )}
                        </div>
                      )
                    })()
                  ) : (
                    <div style={{ maxWidth: '70%' }}>
                      {!m.esPropio && (
                        <div style={{ fontSize: 11, color: '#888', marginBottom: 2 }}>{m.emisorNombre}</div>
                      )}
                      <div style={{
                        background: m.esPropio ? '#1a73e8' : '#e8e8e8',
                        color: m.esPropio ? '#fff' : '#222',
                        padding: '8px 12px',
                        borderRadius: 12,
                        fontSize: 14
                      }}>
                        {m.contenido}
                      </div>
                      <div style={{ fontSize: 10, color: '#aaa', marginTop: 2, textAlign: m.esPropio ? 'right' : 'left' }}>
                        {formatHora(m.creadoEn)}
                      </div>
                    </div>
                  )}
                </div>
              ))}
              <div ref={bottomRef} />
            </div>

            {/* panel de reserva */}
            {chatActivoData && user?.activeProfile === 'RESCATISTA' && reservasActivasChat.length > 0 && (
              <div style={{ padding: '10px 12px', borderTop: '2px solid #f0a500', background: '#fffbf0' }}>
                {reservasActivasChat.map(r => (
                  <div key={r.reservaId} style={{ marginBottom: reservasActivasChat.length > 1 ? 10 : 0 }}>
                    <p style={{ margin: '0 0 8px', fontSize: 13, color: '#333' }}>
                      <strong>{r.animalNombre}</strong> reservado para <strong>{r.adoptanteNombre}</strong>
                    </p>
                    <div style={{ display: 'flex', gap: 8 }}>
                      <button onClick={() => handleConcretar(r.reservaId)} style={{ fontSize: 13, background: '#2e7d32', color: '#fff', border: 'none', padding: '6px 14px', borderRadius: 4, cursor: 'pointer' }}>
                        Adopción concretada
                      </button>
                      <button onClick={() => handleCancelar(r.reservaId)} style={{ fontSize: 13, background: '#c62828', color: '#fff', border: 'none', padding: '6px 14px', borderRadius: 4, cursor: 'pointer' }}>
                        Cancelar reserva
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
            {chatActivoData && user?.activeProfile === 'RESCATISTA' && (
              <div style={{ padding: '8px 12px', borderTop: '1px solid #eee', background: '#fafafa' }}>
                {!mostrarProponer ? (
                  <button onClick={() => setMostrarProponer(true)} style={{ fontSize: 13 }}>
                    Reservar animal para {chatActivoData.otroUsuarioNombre}
                  </button>
                ) : (
                  <div style={{ display: 'flex', gap: 8, alignItems: 'center', flexWrap: 'wrap' }}>
                    <select value={animalSeleccionado} onChange={e => setAnimalSeleccionado(e.target.value)} style={{ fontSize: 13 }}>
                      <option value="">Elegí un animal</option>
                      {animalesDisponibles.map(a => (
                        <option key={a.id} value={a.id}>{a.nombre}</option>
                      ))}
                    </select>
                    <button onClick={handleProponer} disabled={!animalSeleccionado} style={{ fontSize: 13 }}>
                      Confirmar reserva
                    </button>
                    <button onClick={() => setMostrarProponer(false)} style={{ fontSize: 13 }}>
                      Cancelar
                    </button>
                  </div>
                )}
              </div>
            )}
            {chatActivoData && user?.activeProfile === 'ADOPTANTE' && reservasPendientes.length > 0 && (
              <div style={{ padding: '10px 12px', borderTop: '2px solid #f0a500', background: '#fffbf0' }}>
                {reservasPendientes.map(r => (
                  <div key={r.reservaId} style={{ marginBottom: reservasPendientes.length > 1 ? 12 : 0 }}>
                    <p style={{ margin: '0 0 8px', fontSize: 13, color: '#333' }}>
                      <strong>{r.rescatistaNombre}</strong> quiere reservarte a <strong>{r.animalNombre}</strong> para que lo adoptes.
                    </p>
                    <div style={{ display: 'flex', gap: 8 }}>
                      <button onClick={() => handleAceptar(r.reservaId, r.animalNombre)} style={{ fontSize: 13, background: '#2e7d32', color: '#fff', border: 'none', padding: '6px 14px', borderRadius: 4, cursor: 'pointer' }}>
                        Aceptar
                      </button>
                      <button onClick={() => handleRechazar(r.reservaId)} style={{ fontSize: 13, background: '#c62828', color: '#fff', border: 'none', padding: '6px 14px', borderRadius: 4, cursor: 'pointer' }}>
                        Rechazar
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}

            {/* bot de envio (comprador) */}
            {chatActivoData && envioPendiente && envioPendiente.estadoEnvio !== 'CONFIRMADO' && (
              <div style={{ padding: '10px 12px', borderTop: '2px solid #1a73e8', background: '#f0f6ff' }}>
                {errorEnvio && <p style={{ color: 'red', fontSize: 13, margin: '0 0 8px' }}>{errorEnvio}</p>}

                {envioPendiente.estadoEnvio === 'PENDIENTE_METODO' && (() => {
                  const opciones = [
                    envioPendiente.retiroDisponible && { metodo: 'RETIRO_DOMICILIO', label: 'Retiro en el domicilio del vendedor' },
                    { metodo: 'ENVIO_MOTO', label: 'Envío en moto en el día' },
                    { metodo: 'CORREO_ARGENTINO', label: 'Correo Argentino' },
                  ].filter(Boolean)
                  return (
                    <div>
                      <p style={{ margin: '0 0 8px', fontSize: 13, color: '#333' }}>¿Cómo querés recibir tu pedido?</p>
                      <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                        {opciones.map((o, i) => (
                          <button
                            key={o.metodo}
                            disabled={enviandoEnvio}
                            onClick={() => handleElegirMetodoEnvio(o.metodo)}
                            style={{ fontSize: 13, padding: '6px 12px', borderRadius: 4, border: '1px solid #1a73e8', background: '#fff', color: '#1a73e8', cursor: 'pointer' }}
                          >
                            {i + 1}. {o.label}
                          </button>
                        ))}
                      </div>
                    </div>
                  )
                })()}

                {envioPendiente.estadoEnvio === 'PENDIENTE_HORARIO' && (
                  <div>
                    <p style={{ margin: '0 0 8px', fontSize: 13, color: '#333' }}>Elegí el día y horario que te quede mejor para retirar:</p>
                    {envioPendiente.bloquesRetiro.length === 0 ? (
                      <p style={{ fontSize: 13, color: '#888' }}>El vendedor todavía no cargó horarios disponibles.</p>
                    ) : (
                      <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                        {envioPendiente.bloquesRetiro.map(b => (
                          <button
                            key={b.id}
                            disabled={enviandoEnvio}
                            onClick={() => handleElegirHorarioRetiro(b.id)}
                            style={{ fontSize: 13, padding: '6px 12px', borderRadius: 4, border: '1px solid #1a73e8', background: '#fff', color: '#1a73e8', cursor: 'pointer' }}
                          >
                            {DIA_LABEL[b.diaSemana]} {b.horaInicio.substring(0, 5)} a {b.horaFin.substring(0, 5)}hs
                          </button>
                        ))}
                      </div>
                    )}
                    <button
                      type="button"
                      disabled={enviandoEnvio}
                      onClick={handleVolverAElegirMetodo}
                      style={{ fontSize: 12, marginTop: 8, background: 'none', border: 'none', color: '#888', cursor: 'pointer', padding: 0 }}
                    >
                      Volver a elegir método de envío
                    </button>
                  </div>
                )}

                {envioPendiente.estadoEnvio === 'PENDIENTE_DOMICILIO' && (
                  <form onSubmit={handleSubmitDomicilio}>
                    <p style={{ margin: '0 0 8px', fontSize: 13, color: '#333' }}>Completá tu domicilio para el envío:</p>
                    <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginBottom: 8 }}>
                      <input
                        placeholder="Calle *"
                        value={domicilioForm.calle}
                        onChange={e => setDomicilioForm(prev => ({ ...prev, calle: e.target.value }))}
                        style={{ fontSize: 13, padding: '6px 8px', width: 160 }}
                        required
                      />
                      <input
                        placeholder="Altura"
                        value={domicilioForm.altura}
                        onChange={e => setDomicilioForm(prev => ({ ...prev, altura: e.target.value }))}
                        style={{ fontSize: 13, padding: '6px 8px', width: 90 }}
                      />
                      <input
                        placeholder="Piso"
                        value={domicilioForm.piso}
                        onChange={e => setDomicilioForm(prev => ({ ...prev, piso: e.target.value }))}
                        style={{ fontSize: 13, padding: '6px 8px', width: 70 }}
                      />
                      <input
                        placeholder="Depto"
                        value={domicilioForm.depto}
                        onChange={e => setDomicilioForm(prev => ({ ...prev, depto: e.target.value }))}
                        style={{ fontSize: 13, padding: '6px 8px', width: 70 }}
                      />
                    </div>
                    <textarea
                      placeholder="Descripción adicional (opcional)"
                      value={domicilioForm.descripcion}
                      onChange={e => setDomicilioForm(prev => ({ ...prev, descripcion: e.target.value }))}
                      rows={2}
                      style={{ fontSize: 13, padding: '6px 8px', width: '100%', maxWidth: 400, boxSizing: 'border-box', display: 'block', marginBottom: 8 }}
                    />
                    <button type="submit" disabled={enviandoEnvio || !domicilioForm.calle.trim()} style={{ fontSize: 13, padding: '6px 14px' }}>
                      Confirmar domicilio
                    </button>
                    <button
                      type="button"
                      disabled={enviandoEnvio}
                      onClick={handleVolverAElegirMetodo}
                      style={{ fontSize: 12, marginLeft: 10, background: 'none', border: 'none', color: '#888', cursor: 'pointer', padding: 0 }}
                    >
                      Volver a elegir método de envío
                    </button>
                  </form>
                )}
              </div>
            )}

            {/* input de mensaje */}
            <form onSubmit={handleEnviar} style={{ display: 'flex', gap: 8, padding: 12, borderTop: '1px solid #eee' }}>
              <input
                value={texto}
                onChange={e => setTexto(e.target.value)}
                placeholder="Escribí un mensaje..."
                style={{ flex: 1, padding: '8px 12px', borderRadius: 20, border: '1px solid #ccc', fontSize: 14 }}
                disabled={enviando}
              />
              <button type="submit" disabled={enviando || !texto.trim()} style={{ padding: '8px 16px', borderRadius: 20 }}>
                Enviar
              </button>
            </form>
          </>
        )}
      </div>

      {/* modal de motivo de cancelación */}
      {cancelandoReservaId && (
        <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000 }}>
          <div style={{ background: '#fff', color: '#222', padding: '2rem', borderRadius: 8, width: 360, maxWidth: '90%' }}>
            <h3 style={{ marginTop: 0 }}>¿Por qué cancelás la reserva?</h3>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
              <button onClick={() => confirmarCancelacion(cancelandoReservaId, 'NO_CONCRETA')}
                style={{ padding: '10px 14px', textAlign: 'left', background: '#fff3e0', border: '1px solid #f0a500', borderRadius: 4, cursor: 'pointer', fontSize: 13 }}>
                <strong>El adoptante no concretó la adopción</strong>
                <div style={{ fontSize: 11, color: '#888', marginTop: 3 }}>El adoptante no podrá reservar este animal por 1 mes.</div>
              </button>
              <button onClick={() => confirmarCancelacion(cancelandoReservaId, 'ERROR_RESERVA')}
                style={{ padding: '10px 14px', textAlign: 'left', background: '#f5f5f5', border: '1px solid #ccc', borderRadius: 4, cursor: 'pointer', fontSize: 13 }}>
                <strong>Error al reservar</strong>
                <div style={{ fontSize: 11, color: '#888', marginTop: 3 }}>El animal vuelve a estar disponible, sin consecuencias para el adoptante.</div>
              </button>
              <button onClick={() => confirmarCancelacion(cancelandoReservaId, 'PROBLEMA_ANIMAL')}
                style={{ padding: '10px 14px', textAlign: 'left', background: '#f5f5f5', border: '1px solid #ccc', borderRadius: 4, cursor: 'pointer', fontSize: 13 }}>
                <strong>Problema con el animal</strong>
                <div style={{ fontSize: 11, color: '#888', marginTop: 3 }}>El animal vuelve a estar disponible, sin consecuencias para el adoptante.</div>
              </button>
              <button onClick={() => setCancelAndoReservaId(null)} style={{ marginTop: 4, fontSize: 13 }}>
                Volver
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

export default Chats
