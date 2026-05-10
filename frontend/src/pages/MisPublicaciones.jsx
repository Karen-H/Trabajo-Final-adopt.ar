import { useState, useEffect } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { getMisAnimales, cambiarEstadoAnimal, agregarFotosAnimal } from '../api/animal'
import { getMisReportes, resolverReporte } from '../api/reporte'

const ETIQUETA_TIPO = { PERRO: 'Perro', GATO: 'Gato', OTRO: 'Otro' }

const ETIQUETA_EDAD = {
  CACHORRO: 'Cachorro (0-6 meses)',
  JOVEN: 'Joven (6 meses - 2 anios)',
  ADULTO: 'Adulto (2-7 anios)',
  SENIOR: 'Senior (7+ anios)',
}

const ETIQUETA_FOTO_ESTADO = {
  PENDIENTE: 'Pendiente de revision',
  RECHAZADA: 'Rechazada',
}

const ESTADOS_ADOPCION = ['EN_ADOPCION', 'ADOPTADO', 'PERDIDO', 'ENCONTRADO']
const ETIQUETA_ESTADO_ADOPCION = {
  EN_ADOPCION: 'En adopcion',
  ADOPTADO: 'Adoptado',
  PERDIDO: 'Perdido',
  ENCONTRADO: 'Encontrado',
}

function estadoRevision(item) {
  if (item.rechazado) return { texto: 'Rechazado', color: '#c00' }
  if (item.aprobado) return { texto: 'Aprobado', color: '#080' }
  return { texto: 'En revision', color: '#888' }
}

function FotosList({ fotos }) {
  if (!fotos || fotos.length === 0) return null
  return (
    <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', margin: '0.5rem 0' }}>
      {fotos.map(foto => (
        <div key={foto.id} style={{ textAlign: 'center' }}>
          <img
            src={foto.url}
            alt="foto"
            style={{ width: 120, height: 120, objectFit: 'cover', display: 'block' }}
          />
          {ETIQUETA_FOTO_ESTADO[foto.estado] && (
            <small style={{ color: foto.estado === 'RECHAZADA' ? '#c00' : '#888' }}>
              {ETIQUETA_FOTO_ESTADO[foto.estado]}
              {foto.estado === 'RECHAZADA' && foto.motivoRechazo && ': ' + foto.motivoRechazo}
            </small>
          )}
        </div>
      ))}
    </div>
  )
}

function AgregarFotosBtn({ id, rechazado, fotosCount, fotosNuevas, setFotosNuevas, onAgregar }) {
  if (rechazado || fotosCount >= 5) return null
  return (
    <div style={{ margin: '0.5rem 0' }}>
      <input
        type="file"
        accept="image/*"
        multiple
        onChange={e => setFotosNuevas(prev => ({ ...prev, [id]: e.target.files }))}
      />
      <button
        onClick={() => onAgregar(id)}
        disabled={!fotosNuevas[id] || fotosNuevas[id].length === 0}
        style={{ marginLeft: 8 }}
      >
        Agregar fotos
      </button>
    </div>
  )
}

function MisPublicaciones() {
  const navigate = useNavigate()
  const role = localStorage.getItem('role')
  const activeProfile = localStorage.getItem('activeProfile')
  const esRescatista = activeProfile === 'RESCATISTA'

  const [tab, setTab] = useState(esRescatista ? 'adopcion' : 'perdidos')
  const [animales, setAnimales] = useState([])
  const [reportes, setReportes] = useState([])
  const [error, setError] = useState('')
  const [fotosNuevas, setFotosNuevas] = useState({})

  useEffect(() => {
    if (!localStorage.getItem('token')) {
      navigate('/login')
      return
    }
    if (esRescatista) {
      getMisAnimales()
        .then(res => setAnimales(res.data))
        .catch(() => setError('No se pudieron cargar tus animales.'))
    }
    getMisReportes()
      .then(res => setReportes(res.data))
      .catch(() => setError('No se pudieron cargar tus reportes.'))
  }, [navigate, esRescatista])

  async function handleCambiarEstado(id, estado) {
    setError('')
    try {
      const res = await cambiarEstadoAnimal(id, estado)
      setAnimales(prev => prev.map(a => a.id === id ? res.data : a))
    } catch {
      setError('No se pudo cambiar el estado.')
    }
  }

  async function handleAgregarFotosAnimal(id) {
    setError('')
    const files = fotosNuevas[id]
    if (!files || files.length === 0) return
    const formData = new FormData()
    Array.from(files).forEach(f => formData.append('fotos', f))
    try {
      const res = await agregarFotosAnimal(id, formData)
      setAnimales(prev => prev.map(a => a.id === id ? res.data : a))
      setFotosNuevas(prev => ({ ...prev, [id]: null }))
    } catch (err) {
      setError(err.response?.data || 'No se pudieron agregar las fotos.')
    }
  }

  async function handleAgregarFotosReporte(id) {
    setError('')
    const files = fotosNuevas[id]
    if (!files || files.length === 0) return
    const formData = new FormData()
    Array.from(files).forEach(f => formData.append('fotos', f))
    try {
      const res = await agregarFotosAnimal(id, formData)
      setReportes(prev => prev.map(r => r.id === id ? res.data : r))
      setFotosNuevas(prev => ({ ...prev, [id]: null }))
    } catch (err) {
      setError(err.response?.data || 'No se pudieron agregar las fotos.')
    }
  }

  async function handleResolver(id) {
    setError('')
    try {
      const res = await resolverReporte(id)
      setReportes(prev => prev.map(r => r.id === id ? res.data : r))
    } catch (err) {
      setError(err.response?.data || 'No se pudo marcar como resuelto.')
    }
  }

  const perdidos = reportes.filter(r => r.estado === 'PERDIDO')
  const encontrados = reportes.filter(r => r.estado === 'ENCONTRADO')
  const resueltos = reportes.filter(r => r.estado === 'RESUELTO')

  return (
    <div>
      <h2>Mis publicaciones</h2>

      {error && <p style={{ color: 'red' }}>{error}</p>}

      <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginBottom: '1rem' }}>
        {esRescatista && (
          <button onClick={() => setTab('adopcion')} disabled={tab === 'adopcion'}>
            En adopcion ({animales.length})
          </button>
        )}
        <button onClick={() => setTab('perdidos')} disabled={tab === 'perdidos'}>
          Perdidos ({perdidos.length})
        </button>
        <button onClick={() => setTab('encontrados')} disabled={tab === 'encontrados'}>
          Encontrados ({encontrados.length})
        </button>
        <button onClick={() => setTab('resueltos')} disabled={tab === 'resueltos'}>
          Resueltos ({resueltos.length})
        </button>
      </div>

      {tab === 'adopcion' && esRescatista && (
        <div>
          <Link to="/agregar-animal">+ Publicar animal en adopcion</Link>
          <br /><br />
          {animales.length === 0 ? (
            <p>Todavia no publicaste ningun animal en adopcion.</p>
          ) : (
            animales.map(animal => {
              const revision = estadoRevision(animal)
              return (
                <div key={animal.id} style={{ border: '1px solid #ccc', margin: '1rem 0', padding: '1rem' }}>
                  <h3>
                    {animal.nombre}
                    {'  '}
                    <span style={{ fontSize: '0.85rem', color: revision.color }}>
                      [{revision.texto}]
                    </span>
                  </h3>
                  {animal.rechazado && animal.motivoRechazo && (
                    <p style={{ color: '#c00' }}>Motivo de rechazo: {animal.motivoRechazo}</p>
                  )}
                  <p>
                    {ETIQUETA_TIPO[animal.tipo]}
                    {animal.sexo ? ` - ${animal.sexo.charAt(0) + animal.sexo.slice(1).toLowerCase()}` : ''}
                    {animal.edad ? ` - ${ETIQUETA_EDAD[animal.edad]}` : ''}
                  </p>
                  <p>{animal.ciudad}, {animal.provincia}</p>
                  {animal.tipoAdopcion && (
                    <p>Adopcion: {animal.tipoAdopcion === 'PERMANENTE' ? 'Permanente' : 'Transito'}</p>
                  )}
                  <p>
                    Amigable con:{' '}
                    {[
                      animal.amigableConGatos && 'gatos',
                      animal.amigableConPerros && 'perros',
                      animal.amigableConNinos && 'ninos',
                    ].filter(Boolean).join(', ') || 'no especificado'}
                  </p>
                  {animal.descripcion && <p>{animal.descripcion}</p>}
                  <FotosList fotos={animal.fotos} />
                  <AgregarFotosBtn
                    id={animal.id}
                    rechazado={animal.rechazado}
                    fotosCount={animal.fotos.length}
                    fotosNuevas={fotosNuevas}
                    setFotosNuevas={setFotosNuevas}
                    onAgregar={handleAgregarFotosAnimal}
                  />
                  {animal.aprobado && (
                    <div style={{ marginTop: '0.5rem' }}>
                      <strong>Estado: {ETIQUETA_ESTADO_ADOPCION[animal.estado]}</strong>
                      {'  '}
                      <select
                        value={animal.estado}
                        onChange={e => handleCambiarEstado(animal.id, e.target.value)}
                      >
                        {ESTADOS_ADOPCION.map(s => (
                          <option key={s} value={s}>{ETIQUETA_ESTADO_ADOPCION[s]}</option>
                        ))}
                      </select>
                    </div>
                  )}
                </div>
              )
            })
          )}
        </div>
      )}

      {(tab === 'perdidos' || tab === 'encontrados' || tab === 'resueltos') && (
        <div>
          {tab === 'perdidos' && <Link to="/agregar-reporte">+ Publicar perdido / encontrado</Link>}
          <br /><br />
          {(() => {
            const lista = tab === 'perdidos' ? perdidos : tab === 'encontrados' ? encontrados : resueltos
            const etiquetaVacia = {
              perdidos: 'Todavia no publicaste ningun reporte de animal perdido.',
              encontrados: 'Todavia no publicaste ningun reporte de animal encontrado.',
              resueltos: 'No tenes reportes resueltos.',
            }[tab]

            if (lista.length === 0) return <p>{etiquetaVacia}</p>

            return lista.map(reporte => {
              const revision = estadoRevision(reporte)
              const resuelto = reporte.estado === 'RESUELTO'
              return (
                <div key={reporte.id} style={{ border: '1px solid #ccc', margin: '1rem 0', padding: '1rem' }}>
                  <h3>
                    {ETIQUETA_TIPO[reporte.tipo]}
                    {'  '}
                    <span style={{ fontSize: '0.85rem', color: revision.color }}>
                      [{revision.texto}]
                    </span>
                  </h3>
                  {reporte.rechazado && reporte.motivoRechazo && (
                    <p style={{ color: '#c00' }}>Motivo de rechazo: {reporte.motivoRechazo}</p>
                  )}
                  {reporte.direccion && <p>Visto en: {reporte.direccion}</p>}
                  <p>
                    En posesion del publicador:{' '}
                    {reporte.enPosesionDelPublicador ? 'Si' : 'No'}
                  </p>
                  {reporte.descripcion && <p>{reporte.descripcion}</p>}
                  <FotosList fotos={reporte.fotos} />
                  <AgregarFotosBtn
                    id={reporte.id}
                    rechazado={reporte.rechazado}
                    fotosCount={reporte.fotos.length}
                    fotosNuevas={fotosNuevas}
                    setFotosNuevas={setFotosNuevas}
                    onAgregar={handleAgregarFotosReporte}
                  />
                  {reporte.aprobado && !resuelto && (
                    <div style={{ marginTop: '0.5rem' }}>
                      <button onClick={() => handleResolver(reporte.id)}>
                        Marcar como resuelto
                      </button>
                    </div>
                  )}
                </div>
              )
            })
          })()}
        </div>
      )}

      <br />
      <Link to="/">Volver al inicio</Link>
    </div>
  )
}

export default MisPublicaciones
