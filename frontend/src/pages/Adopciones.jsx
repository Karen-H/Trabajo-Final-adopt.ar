import { useState, useEffect, useMemo } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { getAdopciones } from '../api/animal'
import { getFavoritos, agregarFavorito, quitarFavorito } from '../api/favorito'
import FiltroUbicacion from '../components/FiltroUbicacion'
import ModalDenuncia from '../components/ModalDenuncia'
import { iniciarChat } from '../api/chat'
import { getMisBloqueos, getMisReservasAdoptante } from '../api/reserva'
import { useAuth } from '../context/AuthContext'

const TIPOS = ['PERRO', 'GATO', 'OTRO']
const ETIQUETA_TIPO = { PERRO: 'Perro', GATO: 'Gato', OTRO: 'Otro' }
const ETIQUETA_SEXO = { MACHO: 'Macho', HEMBRA: 'Hembra' }
const EDADES = [
  { value: 'CACHORRO', label: 'Cachorro (0-6 meses)' },
  { value: 'JOVEN', label: 'Joven (6 meses - 2 años)' },
  { value: 'ADULTO', label: 'Adulto (2-7 años)' },
  { value: 'SENIOR', label: 'Senior (7+ años)' },
]
const ETIQUETA_EDAD = {
  CACHORRO: 'Cachorro (0-6 meses)',
  JOVEN: 'Joven (6 meses - 2 años)',
  ADULTO: 'Adulto (2-7 años)',
  SENIOR: 'Senior (7+ años)',
}
const TIPOS_ADOPCION = [
  { value: 'PERMANENTE', label: 'Permanente' },
  { value: 'TRANSITO', label: 'Tránsito' },
]
const AMIGABLE_CON = [
  { key: 'amigableConGatos', label: 'Gatos' },
  { key: 'amigableConPerros', label: 'Perros' },
  { key: 'amigableConNinos', label: 'Niños' },
]

function filtrar(animales, tipos, provincia, ciudad, edades, tipoAdopcion, amigableCon, organizacion) {
  return animales.filter(a => {
    const pasaTipo = tipos.length === 0 || tipos.includes(a.tipo)
    const pasaProv = !provincia || (a.provincia || '') === provincia
    const pasaCiudad = !ciudad || (a.ciudad || '') === ciudad
    const pasaEdad = edades.length === 0 || edades.includes(a.edad)
    const pasaTipoAdopcion = !tipoAdopcion || a.tipoAdopcion === tipoAdopcion
    const pasaAmigable = amigableCon.length === 0 || amigableCon.every(k => a[k])
    const pasaOrg = !organizacion || a.organizacion === organizacion
    return pasaTipo && pasaProv && pasaCiudad && pasaEdad && pasaTipoAdopcion && pasaAmigable && pasaOrg
  })
}

function Adopciones() {
  const navigate = useNavigate()
  const { user } = useAuth()
  const estaLogueado = !!localStorage.getItem('token')
  const [animales, setAnimales] = useState([])
  const [cargando, setCargando] = useState(true)
  const [tipos, setTipos] = useState([])
  const [provincia, setProvincia] = useState('')
  const [ciudad, setCiudad] = useState('')
  const [edades, setEdades] = useState([])
  const [tipoAdopcion, setTipoAdopcion] = useState('')
  const [amigableCon, setAmigableCon] = useState([])
  const [organizacion, setOrganizacion] = useState('')
  const [favoritoIds, setFavoritoIds] = useState(new Set())
  const [denunciaAnimalId, setDenunciaAnimalId] = useState(null)
  const [denunciadoIds, setDenunciadoIds] = useState(new Set())
  const [bloqueoIds, setBloqueoIds] = useState(new Set())
  const [misReservas, setMisReservas] = useState([])

  useEffect(() => {
    getAdopciones()
      .then(r => setAnimales(r.data))
      .catch(() => {})
      .finally(() => setCargando(false))
    if (estaLogueado) {
      getFavoritos()
        .then(r => setFavoritoIds(new Set(r.data.map(f => f.animalId))))
        .catch(() => {})
      getMisBloqueos()
        .then(r => setBloqueoIds(new Set(r.data)))
        .catch(() => {})
    }
  }, [])

  // cargar reservas del adoptante cuando el user ya está disponible
  useEffect(() => {
    if (!estaLogueado || user?.activeProfile !== 'ADOPTANTE') return
    getMisReservasAdoptante()
      .then(r => setMisReservas(r.data))
      .catch(() => {})
  }, [user?.activeProfile])

  function toggleTipo(tipo) {
    setTipos(prev => prev.includes(tipo) ? prev.filter(t => t !== tipo) : [...prev, tipo])
  }

  function toggleEdad(edad) {
    setEdades(prev => prev.includes(edad) ? prev.filter(e => e !== edad) : [...prev, edad])
  }

  function toggleAmigable(key) {
    setAmigableCon(prev => prev.includes(key) ? prev.filter(k => k !== key) : [...prev, key])
  }

  async function toggleFavorito(animalId) {
    if (!estaLogueado) {
      localStorage.setItem('pendingFavorito', animalId)
      navigate('/login')
      return
    }
    if (favoritoIds.has(animalId)) {
      await quitarFavorito(animalId)
      setFavoritoIds(prev => { const s = new Set(prev); s.delete(animalId); return s })
    } else {
      await agregarFavorito(animalId)
      setFavoritoIds(prev => new Set([...prev, animalId]))
    }
  }

  const hayFiltros = tipos.length > 0 || provincia || ciudad || edades.length > 0 || tipoAdopcion || amigableCon.length > 0 || organizacion
  const visibles = filtrar(animales, tipos, provincia, ciudad, edades, tipoAdopcion, amigableCon, organizacion)

  const organizaciones = useMemo(() => {
    const set = new Set(animales.map(a => a.organizacion).filter(Boolean))
    return [...set].sort()
  }, [animales])

  if (cargando) return <p>Cargando...</p>

  return (
    <div>
      <h2>Animales en adopcion</h2>

      {/* mis reservas (solo adoptante) */}
      {estaLogueado && user?.activeProfile === 'ADOPTANTE' && misReservas.length > 0 && (
        <div style={{ margin: '0 0 1.5rem' }}>
          <h3 style={{ color: '#1a6e2e' }}>🔒 Mis animales con reserva</h3>
          {misReservas.map(r => {
            const activa = r.estado === 'ACTIVA'
            return (
                <div key={r.reservaId} style={{ border: `2px solid ${activa ? '#4caf50' : '#f0a500'}`, borderRadius: 6, marginBottom: 12, padding: '1rem', background: activa ? '#f1f8f4' : '#fffbf0', color: '#222', display: 'flex', gap: 12, alignItems: 'flex-start', flexWrap: 'wrap' }}>
                {r.fotos?.length > 0 && (
                  <img src={r.fotos[0].url} alt={r.animalNombre} style={{ width: 100, height: 100, objectFit: 'cover', borderRadius: 4, flexShrink: 0 }} />
                )}
                <div>
                  <p style={{ margin: '0 0 4px', fontWeight: 600, fontSize: 15 }}>{r.animalNombre}</p>
                  <p style={{ margin: '0 0 2px', fontSize: 13, color: '#555' }}>
                    {r.tipo && ETIQUETA_TIPO[r.tipo]}{r.sexo ? ' · ' + ETIQUETA_SEXO[r.sexo] : ''}{r.edad ? ' · ' + ETIQUETA_EDAD[r.edad] : ''}
                  </p>
                  {(r.ciudad || r.provincia) && (
                    <p style={{ margin: '0 0 2px', fontSize: 13, color: '#666' }}>{r.ciudad}{r.ciudad && r.provincia ? ', ' : ''}{r.provincia}</p>
                  )}
                  <p style={{ margin: '4px 0 0', fontSize: 13, color: activa ? '#1a6e2e' : '#c47f00' }}>
                    {activa ? '✅ Reserva confirmada' : '⏳ Reserva pendiente de tu confirmación'} · {r.rescatistaNombre}
                  </p>
                </div>
              </div>
            )
          })}
        </div>
      )}

      <div style={{ marginBottom: '1rem', display: 'flex', gap: '1.5rem', alignItems: 'center', flexWrap: 'wrap' }}>
        <div>
          <strong>Tipo:</strong>
          {' '}
          {TIPOS.map(t => (
            <label key={t} style={{ marginRight: 12 }}>
              <input
                type="checkbox"
                checked={tipos.includes(t)}
                onChange={() => toggleTipo(t)}
              />
              {' '}{ETIQUETA_TIPO[t]}
            </label>
          ))}
        </div>
        <FiltroUbicacion
          animales={animales}
          provincia={provincia}
          ciudad={ciudad}
          onProvinciaChange={setProvincia}
          onCiudadChange={setCiudad}
        />
        <div>
          <strong>Edad:</strong>{' '}
          {EDADES.map(e => (
            <label key={e.value} style={{ marginRight: 12 }}>
              <input
                type="checkbox"
                checked={edades.includes(e.value)}
                onChange={() => toggleEdad(e.value)}
              />
              {' '}{e.label}
            </label>
          ))}
        </div>
        <div>
          <strong>Tipo de adopción:</strong>{' '}
          {TIPOS_ADOPCION.map(t => (
            <label key={t.value} style={{ marginRight: 12 }}>
              <input
                type="radio"
                name="tipoAdopcion"
                value={t.value}
                checked={tipoAdopcion === t.value}
                onChange={() => setTipoAdopcion(t.value)}
              />
              {' '}{t.label}
            </label>
          ))}
        </div>
        <div>
          <strong>Amigable con:</strong>{' '}
          {AMIGABLE_CON.map(({ key, label }) => (
            <label key={key} style={{ marginRight: 12 }}>
              <input
                type="checkbox"
                checked={amigableCon.includes(key)}
                onChange={() => toggleAmigable(key)}
              />
              {' '}{label}
            </label>
          ))}
        </div>
        {organizaciones.length > 0 && (
          <div>
            <strong>Organización:</strong>{' '}
            <select value={organizacion} onChange={e => setOrganizacion(e.target.value)}>
              <option value=''>Todas</option>
              {organizaciones.map(o => (
                <option key={o} value={o}>{o}</option>
              ))}
            </select>
          </div>
        )}
        {hayFiltros && (
          <button onClick={() => { setTipos([]); setProvincia(''); setCiudad(''); setEdades([]); setTipoAdopcion(''); setAmigableCon([]); setOrganizacion('') }}>
            Limpiar filtros
          </button>
        )}
      </div>

      {animales.length === 0 ? (
        <p>No hay animales en adopcion en este momento.</p>
      ) : visibles.length === 0 ? (
        <p>No hay resultados para los filtros seleccionados.</p>
      ) : (
        visibles.map(a => (
          <div key={a.id} style={{ border: '1px solid #ccc', marginBottom: 12, padding: 12 }}>
            {a.fotos?.length > 0 && (
              <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap', margin: '6px 0' }}>
                {a.fotos.map(f => (
                  <img key={f.id} src={f.url} alt={a.nombre} style={{ width: 120, height: 120, objectFit: 'cover' }} />
                ))}
              </div>
            )}
            <p>Nombre: {a.nombre}</p>
            <p>Tipo: {ETIQUETA_TIPO[a.tipo]}</p>
            {a.sexo && <p>Sexo: {ETIQUETA_SEXO[a.sexo]}</p>}
            {a.edad && <p>Edad: {ETIQUETA_EDAD[a.edad]}</p>}
            {a.tipoAdopcion && <p>Tipo de adopción: {a.tipoAdopcion === 'PERMANENTE' ? 'Permanente' : 'Tránsito'}</p>}
            {(a.amigableConGatos || a.amigableConPerros || a.amigableConNinos) && (
              <p>Amigable con: {[a.amigableConGatos && 'gatos', a.amigableConPerros && 'perros', a.amigableConNinos && 'niños'].filter(Boolean).join(', ')}</p>
            )}
            {a.descripcion && <p>Descripción: {a.descripcion}</p>}
            <p style={{ fontSize: 12, color: '#666' }}>
              Publicado por {a.rescatistaNombre} en {a.ciudad}, {a.provincia}
            </p>
            {(!estaLogueado || a.usuarioId !== user?.id) && (
              <button
                onClick={() => toggleFavorito(a.id)}
                style={{ background: 'none', border: 'none', cursor: 'pointer', fontSize: 22, padding: '4px 0' }}
                title={favoritoIds.has(a.id) ? 'Quitar de favoritos' : 'Agregar a favoritos'}
              >
                {favoritoIds.has(a.id) ? '❤️' : '🤍'}
              </button>
            )}
            {estaLogueado && a.usuarioId !== user?.id && user?.activeProfile === 'ADOPTANTE' && (
              bloqueoIds.has(a.id) ? (
                <span style={{ fontSize: 12, color: '#999', marginLeft: 8 }}>🚫 No disponible para vos (reserva cancelada)</span>
              ) : (
                <button
                  onClick={async () => {
                    try {
                      await iniciarChat(a.usuarioId, a.id, a.nombre)
                      navigate('/chats')
                    } catch (e) {
                      alert(e.response?.data || 'No se pudo iniciar el chat.')
                    }
                  }}
                  style={{ fontSize: 13, marginLeft: 8 }}
                >
                  💬 Consultar al rescatista
                </button>
              )
            )}
            {estaLogueado && a.usuarioId !== user?.id && (
              <button
                onClick={() => {
                    if (denunciadoIds.has(a.id)) { alert('Ya denunciaste esta publicación'); return }
                    setDenunciaAnimalId(a.id)
                  }}
                style={{ background: 'none', border: 'none', cursor: 'pointer', fontSize: 12, color: '#999', padding: '4px 0', marginLeft: 8 }}
                title="Reportar publicación"
              >
                🚩 Reportar
              </button>
            )}
          </div>
        ))
      )}

      {denunciaAnimalId && (
        <ModalDenuncia
          animalId={denunciaAnimalId}
          onClose={() => setDenunciaAnimalId(null)}
          onSuccess={() => { setDenunciadoIds(prev => new Set([...prev, denunciaAnimalId])); setDenunciaAnimalId(null); alert('Denuncia enviada. El administrador la revisará.') }}
        />
      )}

      <br />
      <Link to="/">Volver al inicio</Link>
    </div>
  )
}

export default Adopciones
