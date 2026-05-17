import { useState, useEffect } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import {
  getAnimalesPendientes, aprobarAnimal, rechazarAnimal,
  getFotosPendientes, aprobarFoto, rechazarFoto,
  getPublicaciones, eliminarAnimalAdmin, eliminarFotoAdmin,
} from '../api/admin'
import {
  getSolicitudesAdmin, aceptarSolicitud, editarLinkSolicitud,
  aprobarSolicitud, rechazarSolicitud, reprogramarSolicitudAdmin,
} from '../api/tienda'
import {
  getItemsPendientesAdmin, aprobarItemAdmin, rechazarItemAdmin,
  getFotosItemPendientesAdmin, aprobarFotoItemAdmin, rechazarFotoItemAdmin,
} from '../api/item'

const TIPOS_ITEM_LABEL = {
  INDUMENTARIA: 'Indumentaria', ACCESORIO: 'Accesorio', ALIMENTO: 'Alimento',
  JUGUETE: 'Juguete', HIGIENE: 'Higiene', CAMA: 'Cama', TRANSPORTE: 'Transporte', OTRO: 'Otro',
}

const MOTIVO_REPROGRAMACION_OPCIONES = [
  { value: 'PROBLEMA_TECNICO', label: 'Problema técnico' },
  { value: 'RESCATISTA_NO_SE_PRESENTO', label: 'Rescatista no se presentó' },
  { value: 'ADMINISTRADOR_NO_SE_PRESENTO', label: 'Administrador no se presentó' },
  { value: 'ADMINISTRADOR_NO_PODRA_PRESENTARSE', label: 'Administrador no podrá presentarse' },
  { value: 'RESCATISTA_SIN_EVIDENCIA', label: 'Rescatista sin evidencia disponible' },
  { value: 'ERROR_EN_HORARIO', label: 'Error en el horario' },
]

const MOTIVO_REPROGRAMACION_LABEL = Object.fromEntries(
  MOTIVO_REPROGRAMACION_OPCIONES.map(o => [o.value, o.label])
)

const ESTADO_SOLICITUD_LABEL = {
  PENDIENTE: 'Pendiente',
  ACEPTADA: 'Aceptada',
  APROBADA: 'Aprobada',
  RECHAZADA: 'Rechazada',
  REPROGRAMADA: 'Reprogramada',
}

function formatFecha(fecha) {
  return new Date(fecha + 'T00:00:00').toLocaleDateString('es-AR')
}

function formatHora(hora) {
  return hora.substring(0, 5) + 'hs'
}

function llamadaTermino(s) {
  const [h, m] = s.horaPreferida.split(':').map(Number)
  const endMs = new Date(`${s.fechaPreferida}T${String(h).padStart(2,'0')}:${String(m).padStart(2,'0')}:00`).getTime() + 30 * 60 * 1000
  return Date.now() >= endMs
}

function horaFinSlot(s) {
  const [h, m] = s.horaPreferida.split(':').map(Number)
  const endMin = h * 60 + m + 30
  return `${String(Math.floor(endMin / 60)).padStart(2, '0')}:${String(endMin % 60).padStart(2, '0')}`
}


const ETIQUETA_TIPO = { PERRO: 'Perro', GATO: 'Gato', OTRO: 'Otro' }
const ETIQUETA_SEXO = { MACHO: 'Macho', HEMBRA: 'Hembra' }
const ETIQUETA_EDAD = {
  CACHORRO: 'Cachorro (0-6 meses)',
  JOVEN: 'Joven (6 meses - 2 años)',
  ADULTO: 'Adulto (2-7 años)',
  SENIOR: 'Senior (7+ años)',
}
const ETIQUETA_ESTADO = { PERDIDO: 'Perdido', ENCONTRADO: 'Encontrado' }

function AccionesAnimal({ id, motivoAnimal, setMotivoAnimal, onAprobar, onRechazar }) {
  return (
    <div style={{ marginTop: '0.75rem', display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
      <button onClick={() => onAprobar(id)}>Aprobar</button>
      <input
        type="text"
        placeholder="Motivo de rechazo"
        value={motivoAnimal[id] || ''}
        onChange={e => setMotivoAnimal(prev => ({ ...prev, [id]: e.target.value }))}
        style={{ width: 280 }}
      />
      <button onClick={() => onRechazar(id)}>Rechazar</button>
    </div>
  )
}

function CardAdopcion({ animal, motivoAnimal, setMotivoAnimal, onAprobar, onRechazar }) {
  return (
    <div style={{ border: '1px solid #ccc', margin: '1rem 0', padding: '1rem' }}>
      <h4>Nombre: {animal.nombre}</h4>
      <p>Tipo: {ETIQUETA_TIPO[animal.tipo]}</p>
      {animal.sexo && <p>Sexo: {ETIQUETA_SEXO[animal.sexo]}</p>}
      {animal.edad && <p>Edad: {ETIQUETA_EDAD[animal.edad]}</p>}
      {animal.tipoAdopcion && <p>Tipo de adopción: {animal.tipoAdopcion === 'PERMANENTE' ? 'Permanente' : 'Tránsito'}</p>}
      <p>Ubicación: {animal.ciudad}, {animal.provincia}</p>
      {(animal.amigableConGatos || animal.amigableConPerros || animal.amigableConNinos) && (
        <p>Amigable con: {[animal.amigableConGatos && 'gatos', animal.amigableConPerros && 'perros', animal.amigableConNinos && 'niños'].filter(Boolean).join(', ')}</p>
      )}
      {animal.descripcion && <p>Descripción: {animal.descripcion}</p>}
      <p>Publicado por: {animal.rescatistaNombre}</p>
      <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', margin: '0.5rem 0' }}>
        {animal.fotos.map(foto => (
          <img key={foto.id} src={foto.url} alt="foto" style={{ width: 150, height: 150, objectFit: 'cover' }} />
        ))}
      </div>
      <AccionesAnimal id={animal.id} motivoAnimal={motivoAnimal} setMotivoAnimal={setMotivoAnimal} onAprobar={onAprobar} onRechazar={onRechazar} />
    </div>
  )
}

function CardReporte({ animal, motivoAnimal, setMotivoAnimal, onAprobar, onRechazar }) {
  return (
    <div style={{ border: '1px solid #ccc', margin: '1rem 0', padding: '1rem' }}>
      <h4>Tipo: {ETIQUETA_TIPO[animal.tipo]} {ETIQUETA_ESTADO[animal.estado] ? `(${ETIQUETA_ESTADO[animal.estado]})` : ''}</h4>
      {animal.direccion && <p>Visto en: {animal.direccion}</p>}
      {animal.fechaAvistamiento && <p>Fecha: {animal.fechaAvistamiento}</p>}
      {animal.estado === 'ENCONTRADO' && <p>En posesión del publicador: {animal.enPosesionDelPublicador ? 'Sí' : 'No'}</p>}
      {animal.descripcion && <p>Descripción: {animal.descripcion}</p>}
      <p>Publicado por: {animal.rescatistaNombre}</p>
      <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', margin: '0.5rem 0' }}>
        {animal.fotos.map(foto => (
          <img key={foto.id} src={foto.url} alt="foto" style={{ width: 150, height: 150, objectFit: 'cover' }} />
        ))}
      </div>
      <AccionesAnimal id={animal.id} motivoAnimal={motivoAnimal} setMotivoAnimal={setMotivoAnimal} onAprobar={onAprobar} onRechazar={onRechazar} />
    </div>
  )
}

function AdminPanel() {
  const navigate = useNavigate()
  const [tab, setTab] = useState('animales')
  const [animales, setAnimales] = useState([])
  const [fotos, setFotos] = useState([])
  const [publicaciones, setPublicaciones] = useState([])
  const [error, setError] = useState('')
  const [motivoAnimal, setMotivoAnimal] = useState({})
  const [motivoFoto, setMotivoFoto] = useState({})
  const [motivoEliminar, setMotivoEliminar] = useState({})
  const [motivoEliminarFoto, setMotivoEliminarFoto] = useState({})

  // tienda
  const [solicitudes, setSolicitudes] = useState([])
  const [linkMap, setLinkMap] = useState({})

  // items
  const [itemsPendientes, setItemsPendientes] = useState([])
  const [fotosItemPendientes, setFotosItemPendientes] = useState([])
  const [motivoRechazarItem, setMotivoRechazarItem] = useState({})
  const [motivoRechazarFotoItem, setMotivoRechazarFotoItem] = useState({})
  const [motivoRechazarMap, setMotivoRechazarMap] = useState({})
  const [motivoReprogramarMap, setMotivoReprogramarMap] = useState({})
  const [llamadaEstado, setLlamadaEstado] = useState({})

  useEffect(() => {
    if (localStorage.getItem('role') !== 'ADMIN') {
      navigate('/')
      return
    }
    cargarAnimales()
    cargarFotos()
    cargarPublicaciones()
    cargarSolicitudesTienda()
    cargarItemsPendientes()
    cargarFotosItemPendientes()
  }, [navigate])

  function cargarItemsPendientes() {
    getItemsPendientesAdmin()
      .then(res => setItemsPendientes(res.data))
      .catch(() => {})
  }

  function cargarFotosItemPendientes() {
    getFotosItemPendientesAdmin()
      .then(res => setFotosItemPendientes(res.data))
      .catch(() => {})
  }

  function cargarAnimales() {
    getAnimalesPendientes()
      .then(res => setAnimales(res.data))
      .catch(() => setError('No se pudieron cargar los animales pendientes.'))
  }

  function cargarFotos() {
    getFotosPendientes()
      .then(res => setFotos(res.data))
      .catch(() => setError('No se pudieron cargar las fotos pendientes.'))
  }

  function cargarPublicaciones() {
    getPublicaciones()
      .then(res => setPublicaciones(res.data))
      .catch(() => {})
  }

  function cargarSolicitudesTienda() {
    getSolicitudesAdmin()
      .then(res => setSolicitudes(res.data))
      .catch(() => {})
  }

  async function handleAceptarSolicitud(id) {
    const link = linkMap[id]
    if (!link?.trim()) { setError('Ingresá el link de llamada antes de aceptar'); return }
    setError('')
    try {
      const res = await aceptarSolicitud(id, link)
      setSolicitudes(prev => prev.map(s => s.id === id ? res.data : s))
    } catch (e) {
      setError(e.response?.data || 'Error al aceptar la solicitud')
    }
  }

  async function handleEditarLink(id) {
    const link = linkMap[id]
    if (!link?.trim()) { setError('Ingresá el nuevo link'); return }
    setError('')
    try {
      const res = await editarLinkSolicitud(id, link)
      setSolicitudes(prev => prev.map(s => s.id === id ? res.data : s))
    } catch (e) {
      setError(e.response?.data || 'Error al editar el link')
    }
  }

  async function handleAprobarSolicitud(id) {
    if (!confirm('¿Aprobar la tienda de este rescatista?')) return
    setError('')
    try {
      const res = await aprobarSolicitud(id)
      setSolicitudes(prev => prev.map(s => s.id === id ? res.data : s))
    } catch (e) {
      setError(e.response?.data || 'Error al aprobar la solicitud')
    }
  }

  async function handleRechazarSolicitud(id) {
    const motivo = motivoRechazarMap[id]
    if (!motivo?.trim()) { setError('Ingresá un motivo de rechazo'); return }
    setError('')
    try {
      const res = await rechazarSolicitud(id, motivo)
      setSolicitudes(prev => prev.map(s => s.id === id ? res.data : s))
    } catch (e) {
      setError(e.response?.data || 'Error al rechazar la solicitud')
    }
  }

  async function handleReprogramarSolicitud(id) {
    const motivo = motivoReprogramarMap[id]
    if (!motivo) { setError('Elegí un motivo de reprogramación'); return }
    setError('')
    try {
      const res = await reprogramarSolicitudAdmin(id, motivo)
      setSolicitudes(prev => prev.map(s => s.id === id ? res.data : s))
    } catch (e) {
      setError(e.response?.data || 'Error al reprogramar')
    }
  }

  async function handleReprogramarConMotivo(id, motivo) {
    setError('')
    try {
      const res = await reprogramarSolicitudAdmin(id, motivo)
      setSolicitudes(prev => prev.map(s => s.id === id ? res.data : s))
    } catch (e) {
      setError(e.response?.data || 'Error al reprogramar')
    }
  }

  async function handleAprobarAnimal(id) {
    setError('')
    try {
      await aprobarAnimal(id)
      setAnimales(prev => prev.filter(a => a.id !== id))
    } catch {
      setError('No se pudo aprobar el animal.')
    }
  }

  async function handleRechazarAnimal(id) {
    setError('')
    const motivo = motivoAnimal[id]
    if (!motivo || !motivo.trim()) {
      setError('Ingresa un motivo de rechazo antes de rechazar.')
      return
    }
    try {
      await rechazarAnimal(id, motivo)
      setAnimales(prev => prev.filter(a => a.id !== id))
    } catch {
      setError('No se pudo rechazar el animal.')
    }
  }

  async function handleAprobarFoto(id) {
    setError('')
    try {
      await aprobarFoto(id)
      setFotos(prev => prev.filter(f => f.id !== id))
    } catch {
      setError('No se pudo aprobar la foto.')
    }
  }

  async function handleRechazarFoto(id) {
    setError('')
    const motivo = motivoFoto[id]
    if (!motivo || !motivo.trim()) {
      setError('Ingresa un motivo de rechazo antes de rechazar.')
      return
    }
    try {
      await rechazarFoto(id, motivo)
      setFotos(prev => prev.filter(f => f.id !== id))
    } catch {
      setError('No se pudo rechazar la foto.')
    }
  }

  async function handleEliminarPublicacion(id) {
    setError('')
    const motivo = motivoEliminar[id]
    if (!motivo || !motivo.trim()) {
      setError('Ingresá un motivo antes de eliminar.')
      return
    }
    if (!window.confirm('¿Eliminear esta publicación? Esta acción no puede deshacerse por el usuario.')) return
    try {
      await eliminarAnimalAdmin(id, motivo)
      setPublicaciones(prev => prev.filter(p => p.id !== id))
    } catch {
      setError('No se pudo eliminar la publicación.')
    }
  }

  async function handleAprobarItem(id) {
    setError('')
    try {
      await aprobarItemAdmin(id)
      setItemsPendientes(prev => prev.filter(it => it.id !== id))
    } catch {
      setError('No se pudo aprobar el ítem.')
    }
  }

  async function handleRechazarItem(id) {
    setError('')
    const motivo = motivoRechazarItem[id]
    if (!motivo?.trim()) { setError('Ingresá un motivo de rechazo.'); return }
    try {
      await rechazarItemAdmin(id, motivo)
      setItemsPendientes(prev => prev.filter(it => it.id !== id))
    } catch {
      setError('No se pudo rechazar el ítem.')
    }
  }

  async function handleAprobarFotoItem(id) {
    setError('')
    try {
      await aprobarFotoItemAdmin(id)
      setFotosItemPendientes(prev => prev.filter(f => f.id !== id))
    } catch {
      setError('No se pudo aprobar la foto.')
    }
  }

  async function handleRechazarFotoItem(id) {
    setError('')
    const motivo = motivoRechazarFotoItem[id]
    if (!motivo?.trim()) { setError('Ingresá un motivo de rechazo.'); return }
    try {
      await rechazarFotoItemAdmin(id, motivo)
      setFotosItemPendientes(prev => prev.filter(f => f.id !== id))
    } catch {
      setError('No se pudo rechazar la foto.')
    }
  }

  async function handleEliminarFotoAdmin(id) {
    setError('')
    const motivo = motivoEliminarFoto[id]
    if (!motivo || !motivo.trim()) {
      setError('Ingresá un motivo antes de eliminar la foto.')
      return
    }
    if (!window.confirm('¿Eliminar esta foto?')) return
    try {
      await eliminarFotoAdmin(id, motivo)
      setPublicaciones(prev => prev.map(p => ({
        ...p,
        fotos: p.fotos.filter(f => f.id !== id)
      })))
    } catch {
      setError('No se pudo eliminar la foto.')
    }
  }

  const adopcionesPendientes = animales.filter(a => a.categoria === 'ADOPCION')
  const reportesPendientes = animales.filter(a => a.categoria === 'PERDIDO_ENCONTRADO')

  // fotos separadas por categoria del animal
  const fotosAdopcion = fotos.filter(f => f.animalCategoria === 'ADOPCION')
  const fotosReporte = fotos.filter(f => f.animalCategoria === 'PERDIDO_ENCONTRADO')

  return (
    <div>
      <h2>Panel de administracion</h2>

      {error && <p style={{ color: 'red' }}>{error}</p>}

      <div>
        <button onClick={() => setTab('animales')} disabled={tab === 'animales'}>
          Animales pendientes ({animales.length})
        </button>
        {' '}
        <button onClick={() => setTab('fotos')} disabled={tab === 'fotos'}>
          Fotos pendientes ({fotos.length})
        </button>
        {' '}
        <button onClick={() => setTab('publicaciones')} disabled={tab === 'publicaciones'}>
          Gestionar publicaciones ({publicaciones.length})
        </button>
        {' '}
        <button onClick={() => setTab('tiendas')} disabled={tab === 'tiendas'}>
          Solicitudes de tienda ({solicitudes.length})
        </button>
        {' '}
        <button onClick={() => setTab('items')} disabled={tab === 'items'}>
          Ítems pendientes ({itemsPendientes.length})
        </button>
        {' '}
        <button onClick={() => setTab('fotos-items')} disabled={tab === 'fotos-items'}>
          Fotos de ítems ({fotosItemPendientes.length})
        </button>
      </div>

      <br />

      {tab === 'animales' && (
        <div>
          {animales.length === 0 && <p>No hay animales pendientes de aprobacion.</p>}

          {adopcionesPendientes.length > 0 && (
            <div>
              <h3>En adopcion ({adopcionesPendientes.length})</h3>
              {adopcionesPendientes.map(animal => (
                <CardAdopcion
                  key={animal.id}
                  animal={animal}
                  motivoAnimal={motivoAnimal}
                  setMotivoAnimal={setMotivoAnimal}
                  onAprobar={handleAprobarAnimal}
                  onRechazar={handleRechazarAnimal}
                />
              ))}
            </div>
          )}

          {reportesPendientes.length > 0 && (
            <div>
              <h3>Perdidos / encontrados ({reportesPendientes.length})</h3>
              {reportesPendientes.map(animal => (
                <CardReporte
                  key={animal.id}
                  animal={animal}
                  motivoAnimal={motivoAnimal}
                  setMotivoAnimal={setMotivoAnimal}
                  onAprobar={handleAprobarAnimal}
                  onRechazar={handleRechazarAnimal}
                />
              ))}
            </div>
          )}
        </div>
      )}

      {tab === 'fotos' && (
        <div>
          {fotos.length === 0 && <p>No hay fotos pendientes de aprobacion.</p>}

          {fotosAdopcion.length > 0 && (
            <div>
              <h3>Fotos de animales en adopcion ({fotosAdopcion.length})</h3>
              {fotosAdopcion.map(foto => (
                <div key={foto.id} style={{ border: '1px solid #ccc', margin: '1rem 0', padding: '1rem', display: 'flex', gap: '1rem', alignItems: 'flex-start' }}>
                  <img src={foto.url} alt="foto" style={{ width: 150, height: 150, objectFit: 'cover', flexShrink: 0 }} />
                  <div>
                    <p><strong>{foto.animalNombre}</strong> ({ETIQUETA_TIPO[foto.animalTipo]})</p>
                    <p>Publicado por: {foto.rescatistaNombre}</p>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
                      <button onClick={() => handleAprobarFoto(foto.id)}>Aprobar</button>
                      <input
                        type="text"
                        placeholder="Motivo de rechazo"
                        value={motivoFoto[foto.id] || ''}
                        onChange={e => setMotivoFoto(prev => ({ ...prev, [foto.id]: e.target.value }))}
                        style={{ width: 250 }}
                      />
                      <button onClick={() => handleRechazarFoto(foto.id)}>Rechazar</button>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}

          {fotosReporte.length > 0 && (
            <div>
              <h3>Fotos de reportes de perdidos / encontrados ({fotosReporte.length})</h3>
              {fotosReporte.map(foto => (
                <div key={foto.id} style={{ border: '1px solid #ccc', margin: '1rem 0', padding: '1rem', display: 'flex', gap: '1rem', alignItems: 'flex-start' }}>
                  <img src={foto.url} alt="foto" style={{ width: 150, height: 150, objectFit: 'cover', flexShrink: 0 }} />
                  <div>
                    <p><strong>{ETIQUETA_TIPO[foto.animalTipo]}</strong> ({foto.animalEstado ? ETIQUETA_ESTADO[foto.animalEstado] : ''})</p>
                    <p>Publicado por: {foto.rescatistaNombre}</p>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
                      <button onClick={() => handleAprobarFoto(foto.id)}>Aprobar</button>
                      <input
                        type="text"
                        placeholder="Motivo de rechazo"
                        value={motivoFoto[foto.id] || ''}
                        onChange={e => setMotivoFoto(prev => ({ ...prev, [foto.id]: e.target.value }))}
                        style={{ width: 250 }}
                      />
                      <button onClick={() => handleRechazarFoto(foto.id)}>Rechazar</button>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      <br />
      <Link to="/">Volver al inicio</Link>

      {tab === 'tiendas' && (
        <div>
          <h3>Solicitudes de tienda</h3>
          {solicitudes.length === 0 && <p>No hay solicitudes de tienda.</p>}
          {solicitudes.map(s => {
            const esMia = s.adminAsignadoId === Number(localStorage.getItem('id') ?? 0) || s.adminAsignadoId != null
            const puedoActuar = s.adminAsignadoId != null && s.adminAsignadoId === Number(localStorage.getItem('id') ?? -1)
            return (
              <div key={s.id} style={{ border: '1px solid #ccc', margin: '1rem 0', padding: '1rem' }}>
                <p><strong>Estado:</strong> {ESTADO_SOLICITUD_LABEL[s.estado]}</p>
                <p><strong>Rescatista:</strong> {s.rescatistaNombre} {s.rescatistaApellido} — {s.rescatistaEmail} — {s.rescatistaTel}</p>
                {s.rescatistaOrganizacion && <p><strong>Organización:</strong> {s.rescatistaOrganizacion}</p>}
                <p><strong>Turno solicitado:</strong> {formatFecha(s.fechaPreferida)} a las {formatHora(s.horaPreferida)}</p>
                {s.adminAsignadoNombre
                  ? <p><strong>Asignado a:</strong> {s.adminAsignadoNombre} {s.adminAsignadoApellido}</p>
                  : <p><strong>Asignado a:</strong> sin asignar (pendiente de reprogramación por rescatista)</p>
                }
                {s.linkLlamada && <p><strong>Link:</strong> <a href={s.linkLlamada} target="_blank" rel="noreferrer">{s.linkLlamada}</a></p>}
                {s.motivoReprogramacion && <p><strong>Motivo reprogramación:</strong> {MOTIVO_REPROGRAMACION_LABEL[s.motivoReprogramacion]}</p>}
                {s.motivoRechazo && <p><strong>Motivo rechazo:</strong> {s.motivoRechazo}</p>}

                {/* acciones: solo si esta solicitud está asignada a este admin */}
                {s.estado === 'PENDIENTE' && s.adminAsignadoId != null && (
                  <div style={{ marginTop: 8 }}>
                    <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', alignItems: 'center', marginBottom: 8 }}>
                      <input
                        type="text"
                        placeholder="Link de videollamada"
                        value={linkMap[s.id] || ''}
                        onChange={e => setLinkMap(prev => ({ ...prev, [s.id]: e.target.value }))}
                        style={{ width: 300 }}
                      />
                      <button onClick={() => handleAceptarSolicitud(s.id)}>Aceptar llamada</button>
                    </div>
                    <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', alignItems: 'center' }}>
                      <input
                        type="text"
                        placeholder="Motivo de rechazo"
                        value={motivoRechazarMap[s.id] || ''}
                        onChange={e => setMotivoRechazarMap(prev => ({ ...prev, [s.id]: e.target.value }))}
                        style={{ width: 250 }}
                      />
                      <button onClick={() => handleRechazarSolicitud(s.id)}>Rechazar</button>
                      <select
                        value={motivoReprogramarMap[s.id] || ''}
                        onChange={e => setMotivoReprogramarMap(prev => ({ ...prev, [s.id]: e.target.value }))}
                      >
                        <option value="">-- Motivo reprogramación --</option>
                        {MOTIVO_REPROGRAMACION_OPCIONES.map(o => (
                          <option key={o.value} value={o.value}>{o.label}</option>
                        ))}
                      </select>
                      <button onClick={() => handleReprogramarSolicitud(s.id)}>Reprogramar</button>
                    </div>
                  </div>
                )}

                {s.estado === 'ACEPTADA' && s.adminAsignadoId != null && (() => {
                  const paso = llamadaEstado[s.id]
                  const terminada = llamadaTermino(s)
                  const setPaso = v => setLlamadaEstado(prev => ({ ...prev, [s.id]: v }))
                  return (
                    <div style={{ marginTop: 8 }}>

                      {/* Editar link — siempre visible */}
                      <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', alignItems: 'center', marginBottom: 8 }}>
                        <input
                          type="text"
                          placeholder="Editar link de llamada"
                          value={linkMap[s.id] || s.linkLlamada || ''}
                          onChange={e => setLinkMap(prev => ({ ...prev, [s.id]: e.target.value }))}
                          style={{ width: 300 }}
                        />
                        <button onClick={() => handleEditarLink(s.id)}>Actualizar link</button>
                      </div>

                      {/* Reprogramación anticipada (solo si no estamos en el flujo post-llamada) */}
                      {!paso && (
                        <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', alignItems: 'center', marginBottom: 8 }}>
                          <select
                            value={motivoReprogramarMap[s.id] || ''}
                            onChange={e => setMotivoReprogramarMap(prev => ({ ...prev, [s.id]: e.target.value }))}
                          >
                            <option value="">-- Reprogramar antes de la llamada --</option>
                            {MOTIVO_REPROGRAMACION_OPCIONES.filter(o =>
                              ['ADMINISTRADOR_NO_PODRA_PRESENTARSE', 'ERROR_EN_HORARIO', 'PROBLEMA_TECNICO'].includes(o.value)
                            ).map(o => (
                              <option key={o.value} value={o.value}>{o.label}</option>
                            ))}
                          </select>
                          <button onClick={() => handleReprogramarSolicitud(s.id)}>Reprogramar</button>
                        </div>
                      )}

                      {/* Botón principal: habilitado solo cuando terminó el slot */}
                      {!paso && (
                        <div style={{ marginTop: 4 }}>
                          <button
                            disabled={!terminada}
                            onClick={() => setPaso('pregunta')}
                            title={!terminada ? `Disponible a partir de las ${horaFinSlot(s)}hs` : ''}
                          >
                            ¿La llamada se llevó a cabo?
                          </button>
                          {!terminada && (
                            <span style={{ marginLeft: 8, color: '#888', fontSize: '0.85em' }}>
                              Habilitado a las {horaFinSlot(s)}hs del {formatFecha(s.fechaPreferida)}
                            </span>
                          )}
                        </div>
                      )}

                      {/* Paso 1: Sí / No */}
                      {paso === 'pregunta' && (
                        <div style={{ marginTop: 8 }}>
                          <strong>¿La llamada se llevó a cabo?</strong>
                          <div style={{ marginTop: 6, display: 'flex', gap: 8 }}>
                            <button onClick={() => setPaso('si')}>Sí</button>
                            <button onClick={() => setPaso('no')}>No</button>
                            <button onClick={() => setPaso(null)} style={{ color: '#888' }}>Cancelar</button>
                          </div>
                        </div>
                      )}

                      {/* Paso 2a: No → ¿Quién no se presentó? */}
                      {paso === 'no' && (
                        <div style={{ marginTop: 8 }}>
                          <strong>¿Quién no se presentó?</strong>
                          <div style={{ marginTop: 6, display: 'flex', gap: 8 }}>
                            <button onClick={() => setPaso('no_admin')}>El administrador</button>
                            <button onClick={() => setPaso('no_rescatista')}>El rescatista</button>
                            <button onClick={() => setPaso('pregunta')} style={{ color: '#888' }}>Volver</button>
                          </div>
                        </div>
                      )}

                      {/* Paso 3a: Admin no se presentó → solo Reprogramar */}
                      {paso === 'no_admin' && (
                        <div style={{ marginTop: 8 }}>
                          <p style={{ margin: '0 0 6px' }}>El administrador no se presentó. La solicitud volverá a estar disponible para el rescatista.</p>
                          <div style={{ display: 'flex', gap: 8 }}>
                            <button onClick={() => handleReprogramarConMotivo(s.id, 'ADMINISTRADOR_NO_SE_PRESENTO')}>Reprogramar</button>
                            <button onClick={() => setPaso('no')} style={{ color: '#888' }}>Volver</button>
                          </div>
                        </div>
                      )}

                      {/* Paso 3b: Rescatista no se presentó → Reprogramar o Rechazar */}
                      {paso === 'no_rescatista' && (
                        <div style={{ marginTop: 8 }}>
                          <p style={{ margin: '0 0 6px' }}>El rescatista no se presentó.</p>
                          <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', alignItems: 'center' }}>
                            <button onClick={() => handleReprogramarConMotivo(s.id, 'RESCATISTA_NO_SE_PRESENTO')}>Reprogramar</button>
                            <input
                              type="text"
                              placeholder="Motivo de rechazo"
                              value={motivoRechazarMap[s.id] || ''}
                              onChange={e => setMotivoRechazarMap(prev => ({ ...prev, [s.id]: e.target.value }))}
                              style={{ width: 250 }}
                            />
                            <button onClick={() => handleRechazarSolicitud(s.id)}>Rechazar</button>
                            <button onClick={() => setPaso('no')} style={{ color: '#888' }}>Volver</button>
                          </div>
                        </div>
                      )}

                      {/* Paso 2b: Sí → todos los botones */}
                      {paso === 'si' && (
                        <div style={{ marginTop: 8 }}>
                          <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', alignItems: 'center' }}>
                            <button onClick={() => handleAprobarSolicitud(s.id)}>Aprobar tienda</button>
                            <input
                              type="text"
                              placeholder="Motivo de rechazo"
                              value={motivoRechazarMap[s.id] || ''}
                              onChange={e => setMotivoRechazarMap(prev => ({ ...prev, [s.id]: e.target.value }))}
                              style={{ width: 250 }}
                            />
                            <button onClick={() => handleRechazarSolicitud(s.id)}>Rechazar</button>
                            <select
                              value={motivoReprogramarMap[s.id] || ''}
                              onChange={e => setMotivoReprogramarMap(prev => ({ ...prev, [s.id]: e.target.value }))}
                            >
                              <option value="">-- Motivo reprogramación --</option>
                              {MOTIVO_REPROGRAMACION_OPCIONES.map(o => (
                                <option key={o.value} value={o.value}>{o.label}</option>
                              ))}
                            </select>
                            <button onClick={() => handleReprogramarSolicitud(s.id)}>Reprogramar</button>
                          </div>
                        </div>
                      )}

                    </div>
                  )
                })()}
              </div>
            )
          })}
        </div>
      )}

      {tab === 'items' && (
        <div>
          <h3>Ítems pendientes de aprobación</h3>
          {itemsPendientes.length === 0 && <p>No hay ítems pendientes.</p>}
          {itemsPendientes.map(item => (
            <div key={item.id} style={{ border: '1px solid #ccc', margin: '1rem 0', padding: '1rem' }}>
              <p><strong>{item.titulo}</strong> — {TIPOS_ITEM_LABEL[item.tipo] ?? item.tipo}</p>
              {item.precio != null && <p>Precio: ${Number(item.precio).toLocaleString('es-AR')}</p>}
              {item.descripcion && <p>Descripción: {item.descripcion}</p>}
              <p>Publicado por: {item.rescatistaNombre}</p>
              <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', margin: '0.5rem 0' }}>
                {item.fotos.map(foto => (
                  <img key={foto.id} src={foto.url} alt="foto" style={{ width: 120, height: 120, objectFit: 'cover' }} />
                ))}
              </div>
              <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', alignItems: 'center' }}>
                <button onClick={() => handleAprobarItem(item.id)}>Aprobar</button>
                <input
                  type="text"
                  placeholder="Motivo de rechazo"
                  value={motivoRechazarItem[item.id] || ''}
                  onChange={e => setMotivoRechazarItem(prev => ({ ...prev, [item.id]: e.target.value }))}
                  style={{ width: 260 }}
                />
                <button onClick={() => handleRechazarItem(item.id)}>Rechazar</button>
              </div>
            </div>
          ))}
        </div>
      )}

      {tab === 'fotos-items' && (
        <div>
          <h3>Fotos de ítems pendientes de aprobación</h3>
          {fotosItemPendientes.length === 0 && <p>No hay fotos pendientes.</p>}
          {fotosItemPendientes.map(foto => (
            <div key={foto.id} style={{ border: '1px solid #ccc', margin: '1rem 0', padding: '1rem', display: 'flex', gap: '1rem', alignItems: 'flex-start' }}>
              <img src={foto.url} alt="foto" style={{ width: 150, height: 150, objectFit: 'cover', flexShrink: 0 }} />
              <div>
                <p><strong>{foto.itemTitulo}</strong> ({TIPOS_ITEM_LABEL[foto.itemTipo] ?? foto.itemTipo})</p>
                <p>Publicado por: {foto.rescatistaNombre}</p>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
                  <button onClick={() => handleAprobarFotoItem(foto.id)}>Aprobar</button>
                  <input
                    type="text"
                    placeholder="Motivo de rechazo"
                    value={motivoRechazarFotoItem[foto.id] || ''}
                    onChange={e => setMotivoRechazarFotoItem(prev => ({ ...prev, [foto.id]: e.target.value }))}
                    style={{ width: 250 }}
                  />
                  <button onClick={() => handleRechazarFotoItem(foto.id)}>Rechazar</button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {tab === 'publicaciones' && (
        <div>
          {publicaciones.length === 0 && <p>No hay publicaciones activas.</p>}
          {publicaciones.map(pub => {
            const esReporte = pub.categoria === 'PERDIDO_ENCONTRADO'
            return (
              <div key={pub.id} style={{ border: '1px solid #ccc', margin: '1rem 0', padding: '1rem' }}>
                <h4>
                  {esReporte
                    ? `${ETIQUETA_TIPO[pub.tipo]} (${ETIQUETA_ESTADO[pub.estado] || pub.estado})`
                    : pub.nombre}
                </h4>
                <p>Publicado por: {pub.rescatistaNombre}</p>
                {pub.provincia && <p>Ubicación: {pub.ciudad}, {pub.provincia}</p>}
                <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap', margin: '0.5rem 0' }}>
                  {pub.fotos?.filter(f => f.estado === 'APROBADA').map(foto => (
                    <div key={foto.id} style={{ textAlign: 'center' }}>
                      <img src={foto.url} alt="foto" style={{ width: 100, height: 100, objectFit: 'cover', display: 'block' }} />
                      <input
                        type="text"
                        placeholder="Motivo"
                        value={motivoEliminarFoto[foto.id] || ''}
                        onChange={e => setMotivoEliminarFoto(prev => ({ ...prev, [foto.id]: e.target.value }))}
                        style={{ width: 95, fontSize: 11 }}
                      />
                      <button onClick={() => handleEliminarFotoAdmin(foto.id)} style={{ fontSize: 11, color: '#c00' }}>
                        Eliminar foto
                      </button>
                    </div>
                  ))}
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginTop: '0.5rem' }}>
                  <input
                    type="text"
                    placeholder="Motivo de eliminación"
                    value={motivoEliminar[pub.id] || ''}
                    onChange={e => setMotivoEliminar(prev => ({ ...prev, [pub.id]: e.target.value }))}
                    style={{ width: 280 }}
                  />
                  <button onClick={() => handleEliminarPublicacion(pub.id)} style={{ color: '#c00' }}>
                    Eliminar publicación
                  </button>
                </div>
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}

export default AdminPanel
