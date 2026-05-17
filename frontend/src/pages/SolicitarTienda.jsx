import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import {
  getSlots,
  getMiSolicitud,
  crearSolicitud,
  editarSolicitud,
  cancelarSolicitud,
  reprogramarComoRescatista,
} from '../api/tienda'

const MOTIVO_REPROGRAMACION = {
  PROBLEMA_TECNICO: 'Problema técnico',
  RESCATISTA_NO_SE_PRESENTO: 'Rescatista no se presentó',
  ADMINISTRADOR_NO_SE_PRESENTO: 'Administrador no se presentó',
  ADMINISTRADOR_NO_PODRA_PRESENTARSE: 'Administrador no podrá presentarse',
  RESCATISTA_SIN_EVIDENCIA: 'Rescatista sin evidencia disponible',
  ERROR_EN_HORARIO: 'Error en el horario',
}

const ESTADO_LABEL = {
  PENDIENTE: 'Pendiente',
  ACEPTADA: 'Aceptada',
  APROBADA: 'Aprobada',
  RECHAZADA: 'Rechazada',
  REPROGRAMADA: 'Reprogramada',
}

const CONDICIONES = [
  'La tienda será usada para sustentar el rescate de animales.',
  'No se aceptan negocios no vinculados al rescate.',
  'En la videollamada deberás mostrar evidencia de tu actividad de rescate.',
]

function formatSlot(slot) {
  const fecha = new Date(slot.fecha + 'T00:00:00')
  const dias = ['Domingo', 'Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado']
  const dia = dias[fecha.getDay()]
  const fechaStr = fecha.toLocaleDateString('es-AR')
  const hora = slot.hora.substring(0, 5)
  return `${dia} ${fechaStr} a las ${hora}hs`
}

function formatFechaSel(fechaStr) {
  const fecha = new Date(fechaStr + 'T00:00:00')
  const dias = ['Domingo', 'Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado']
  return `${dias[fecha.getDay()]} ${fecha.toLocaleDateString('es-AR')}`
}

function SlotSelector({ slots, value, onChange }) {
  const fechaDeValue = value ? value.split('|')[0] : ''
  const horaDeValue = value ? value.split('|')[1] : ''
  const [fechaElegida, setFechaElegida] = useState(fechaDeValue)
  const [horaElegida, setHoraElegida] = useState(horaDeValue)

  useEffect(() => {
    if (!value) { setFechaElegida(''); setHoraElegida('') }
  }, [value])

  const fechasDisponibles = [...new Set(slots.map(s => s.fecha))].sort()
  const horasDisponibles = slots.filter(s => s.fecha === fechaElegida).map(s => s.hora).sort()

  function handleFechaChange(e) {
    setFechaElegida(e.target.value)
    setHoraElegida('')
    onChange('')
  }

  function handleHoraChange(e) {
    const h = e.target.value
    setHoraElegida(h)
    onChange(h ? `${fechaElegida}|${h}` : '')
  }

  return (
    <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginTop: 4 }}>
      <select value={fechaElegida} onChange={handleFechaChange}>
        <option value="">-- Elegí un día --</option>
        {fechasDisponibles.map(f => <option key={f} value={f}>{formatFechaSel(f)}</option>)}
      </select>
      <select value={horaElegida} onChange={handleHoraChange} disabled={!fechaElegida}>
        <option value="">-- Elegí un horario --</option>
        {horasDisponibles.map(h => <option key={h} value={h}>{h.substring(0, 5)}hs</option>)}
      </select>
    </div>
  )
}

function SolicitarTienda() {
  const { user } = useAuth()
  const navigate = useNavigate()

  const [solicitud, setSolicitud] = useState(null)
  const [slots, setSlots] = useState([])
  const [slotElegido, setSlotElegido] = useState('')
  const [condiciones, setCondiciones] = useState([false, false, false])
  const [mostrarModal, setMostrarModal] = useState(false)
  const [editando, setEditando] = useState(false)
  const [reprogramando, setReprogramando] = useState(false)
  const [error, setError] = useState('')
  const [cargando, setCargando] = useState(true)

  useEffect(() => {
    if (!localStorage.getItem('token')) {
      navigate('/login')
      return
    }
    if (user?.activeProfile !== 'RESCATISTA') {
      navigate('/')
      return
    }

    Promise.all([getMiSolicitud().catch(() => null), getSlots()])
      .then(([solRes, slotsRes]) => {
        if (solRes) setSolicitud(solRes.data)
        setSlots(slotsRes.data)
      })
      .finally(() => setCargando(false))
  }, [navigate, user])

  function todasCondicionesAceptadas() {
    return condiciones.every(Boolean)
  }

  function parseSlot(valor) {
    const [fecha, hora] = valor.split('|')
    return { fechaPreferida: fecha, horaPreferida: hora }
  }

  async function handleCrear() {
    if (!slotElegido) { setError('Elegí un horario disponible'); return }
    if (!todasCondicionesAceptadas()) { setError('Debés aceptar todas las condiciones'); return }
    setError('')
    try {
      const res = await crearSolicitud(parseSlot(slotElegido))
      setSolicitud(res.data)
      setMostrarModal(false)
    } catch (e) {
      setError(e.response?.data || 'Error al crear la solicitud')
    }
  }

  async function handleEditar() {
    if (!slotElegido) { setError('Elegí un horario disponible'); return }
    setError('')
    try {
      const res = await editarSolicitud(parseSlot(slotElegido))
      setSolicitud(res.data)
      setEditando(false)
    } catch (e) {
      setError(e.response?.data || 'Error al editar la solicitud')
    }
  }

  async function handleReprogramar() {
    if (!slotElegido) { setError('Elegí un horario disponible'); return }
    if (!todasCondicionesAceptadas()) { setError('Debés aceptar todas las condiciones'); return }
    setError('')
    try {
      const res = await reprogramarComoRescatista(parseSlot(slotElegido))
      setSolicitud(res.data)
      setReprogramando(false)
    } catch (e) {
      setError(e.response?.data || 'Error al reprogramar')
    }
  }

  async function handleCancelar() {
    if (!confirm('¿Cancelar la solicitud de tienda?')) return
    try {
      await cancelarSolicitud()
      setSolicitud(null)
    } catch (e) {
      setError(e.response?.data || 'Error al cancelar')
    }
  }

  if (cargando) return <p>Cargando...</p>

  // si ya tiene tienda aprobada
  if (user?.tieneTienda) {
    return (
      <div>
        <h2>Mi tienda</h2>
        <p>Tu tienda ya está habilitada.</p>
      </div>
    )
  }

  // si no tiene solicitud activa, mostrar botón para abrir
  if (!solicitud) {
    return (
      <div>
        <h2>Abrir tienda</h2>
        <p>
          Podés solicitar la apertura de tu tienda para vender productos
          relacionados con el rescate de animales.
        </p>
        <button onClick={() => { setMostrarModal(true); setError('') }}>
          Solicitar apertura de tienda
        </button>

        {mostrarModal && (
          <div style={{ border: '1px solid #ccc', padding: 16, marginTop: 16 }}>
            <h3>Nueva solicitud</h3>

            <div style={{ marginBottom: 12 }}>
              <label>Horario disponible</label>
              <SlotSelector slots={slots} value={slotElegido} onChange={setSlotElegido} />
            </div>

            <div style={{ marginBottom: 12 }}>
              <p style={{ fontWeight: 'bold', marginBottom: 8 }}>
                Antes de continuar, confirmá que:
              </p>
              {CONDICIONES.map((texto, i) => (
                <label key={i} style={{ display: 'flex', gap: 8, marginBottom: 6 }}>
                  <input
                    type="checkbox"
                    checked={condiciones[i]}
                    onChange={e => {
                      const copia = [...condiciones]
                      copia[i] = e.target.checked
                      setCondiciones(copia)
                    }}
                  />
                  {texto}
                </label>
              ))}
            </div>

            {error && <p style={{ color: 'red' }}>{error}</p>}

            <button onClick={handleCrear} disabled={!todasCondicionesAceptadas()}>
              Confirmar solicitud
            </button>
            <button onClick={() => setMostrarModal(false)} style={{ marginLeft: 8 }}>
              Cancelar
            </button>
          </div>
        )}
      </div>
    )
  }

  // tiene solicitud activa
  return (
    <div>
      <h2>Solicitud de tienda</h2>

      <div style={{ border: '1px solid #ccc', padding: 16, marginBottom: 16 }}>
        <p><strong>Estado:</strong> {ESTADO_LABEL[solicitud.estado]}</p>
        <p>
          <strong>Turno solicitado:</strong>{' '}
          {formatSlot({ fecha: solicitud.fechaPreferida, hora: solicitud.horaPreferida })}
        </p>
        {solicitud.linkLlamada && (
          <p>
            <strong>Link de videollamada:</strong>{' '}
            <a href={solicitud.linkLlamada} target="_blank" rel="noreferrer">
              {solicitud.linkLlamada}
            </a>
          </p>
        )}
        {solicitud.motivoReprogramacion && (
          <p>
            <strong>Motivo de reprogramación:</strong>{' '}
            {MOTIVO_REPROGRAMACION[solicitud.motivoReprogramacion]}
          </p>
        )}
        {solicitud.motivoRechazo && (
          <p>
            <strong>Motivo de rechazo:</strong> {solicitud.motivoRechazo}
          </p>
        )}
        {solicitud.bloqueadoHasta && (
          <p style={{ color: 'red' }}>
            Podés volver a solicitar una tienda a partir del{' '}
            {new Date(solicitud.bloqueadoHasta + 'T00:00:00').toLocaleDateString('es-AR')}
          </p>
        )}
      </div>

      {/* acciones según estado */}
      {solicitud.estado === 'PENDIENTE' && !editando && (
        <div style={{ display: 'flex', gap: 8 }}>
          <button onClick={() => { setEditando(true); setSlotElegido(''); setError('') }}>
            Editar horario
          </button>
          <button onClick={handleCancelar}>Cancelar solicitud</button>
        </div>
      )}

      {solicitud.estado === 'PENDIENTE' && editando && (
        <div style={{ border: '1px solid #ccc', padding: 16, marginTop: 12 }}>
          <h3>Cambiar horario</h3>
          <SlotSelector slots={slots} value={slotElegido} onChange={setSlotElegido} />
          {error && <p style={{ color: 'red' }}>{error}</p>}
          <button onClick={handleEditar}>Guardar</button>
          <button onClick={() => setEditando(false)} style={{ marginLeft: 8 }}>Cancelar</button>
        </div>
      )}

      {solicitud.estado === 'REPROGRAMADA' && !reprogramando && (
        <div>
          <p>
            La llamada fue reprogramada. Elegí un nuevo horario para reagendarla.
          </p>
          <button onClick={() => { setReprogramando(true); setSlotElegido(''); setCondiciones([false, false, false]); setError('') }}>
            Elegir nuevo horario
          </button>
        </div>
      )}

      {solicitud.estado === 'REPROGRAMADA' && reprogramando && (
        <div style={{ border: '1px solid #ccc', padding: 16, marginTop: 12 }}>
          <h3>Nuevo horario</h3>
          <SlotSelector slots={slots} value={slotElegido} onChange={setSlotElegido} />

          <div style={{ marginBottom: 12 }}>
            <p style={{ fontWeight: 'bold', marginBottom: 8 }}>Confirmá nuevamente las condiciones:</p>
            {CONDICIONES.map((texto, i) => (
              <label key={i} style={{ display: 'flex', gap: 8, marginBottom: 6 }}>
                <input
                  type="checkbox"
                  checked={condiciones[i]}
                  onChange={e => {
                    const copia = [...condiciones]
                    copia[i] = e.target.checked
                    setCondiciones(copia)
                  }}
                />
                {texto}
              </label>
            ))}
          </div>

          {error && <p style={{ color: 'red' }}>{error}</p>}
          <button onClick={handleReprogramar} disabled={!todasCondicionesAceptadas()}>
            Confirmar nuevo horario
          </button>
          <button onClick={() => setReprogramando(false)} style={{ marginLeft: 8 }}>Cancelar</button>
        </div>
      )}
    </div>
  )
}

export default SolicitarTienda
