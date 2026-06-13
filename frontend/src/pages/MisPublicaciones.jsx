import { useState, useEffect } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { getMisAnimales, cambiarEstadoAnimal, agregarFotosAnimal, pausarPublicacion, eliminarPublicacionPermanente, reactivarPublicacion, eliminarFotoAnimal } from '../api/animal'
import { getMisReportes, resolverReporte } from '../api/reporte'
import { getMisReservasActivas, concretarReserva, cancelarReserva } from '../api/reserva'

const ETIQUETA_TIPO = { PERRO: 'Perro', GATO: 'Gato', OTRO: 'Otro' }
const ETIQUETA_SEXO = { MACHO: 'Macho', HEMBRA: 'Hembra' }

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



function StatsAnimal({ animal }) {
  if (animal.vistas == null) return null
  return (
    <p style={{ margin: '6px 0 0', fontSize: 12, color: '#666' }}>
      👁 {animal.vistas} vistas · ❤️ {animal.cantidadFavoritos} favoritos · 💬 {animal.cantidadChats} chats
    </p>
  )
}

function estadoRevision(item) {
  if (item.rechazado) return { texto: 'Rechazado', color: '#c00' }
  if (item.aprobado) return { texto: 'Aprobado', color: '#080' }
  return { texto: 'En revision', color: '#888' }
}

function FotosList({ fotos, onEliminar }) {
  if (!fotos || fotos.length === 0) return null
  const fotosActivas = fotos.filter(f => f.estado !== 'ELIMINADA').length
  return (
    <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', margin: '0.5rem 0' }}>
      {fotos.map(foto => (
        <div key={foto.id} style={{ textAlign: 'center' }}>
          <img
            src={foto.url}
            alt="foto"
            style={{ width: 120, height: 120, objectFit: 'cover', display: 'block', opacity: foto.estado === 'ELIMINADA' ? 0.4 : 1 }}
          />
          {foto.estado === 'ELIMINADA' ? (
            <small style={{ color: '#c00' }}>
              {foto.motivoRechazo ? `Eliminada por admin: ${foto.motivoRechazo}` : 'Eliminada'}
            </small>
          ) : (
            <>
              {ETIQUETA_FOTO_ESTADO[foto.estado] && (
                <small style={{ color: foto.estado === 'RECHAZADA' ? '#c00' : '#888' }}>
                  {ETIQUETA_FOTO_ESTADO[foto.estado]}
                  {foto.estado === 'RECHAZADA' && foto.motivoRechazo && ': ' + foto.motivoRechazo}
                </small>
              )}
              {onEliminar && fotosActivas > 1 && (
                <button
                  onClick={() => onEliminar(foto.id)}
                  style={{ display: 'block', margin: '2px auto', fontSize: 11 }}
                >
                  Eliminar foto
                </button>
              )}
            </>
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

  const [tab, setTab] = useState(esRescatista ? 'en_revision' : 'perdidos')
  const [animales, setAnimales] = useState([])
  const [reportes, setReportes] = useState([])
  const [reservas, setReservas] = useState([])
  const [cancelandoReservaId, setCancelandoReservaId] = useState(null)
  const [modalPausarEliminarId, setModalPausarEliminarId] = useState(null)
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
      getMisReservasActivas()
        .then(res => setReservas(res.data))
        .catch(() => {})
    }
    getMisReportes()
      .then(res => setReportes(res.data))
      .catch(() => setError('No se pudieron cargar tus reportes.'))
  }, [navigate, esRescatista])

  function cargarReservas() {
    getMisReservasActivas()
      .then(res => setReservas(res.data))
      .catch(() => {})
  }

  async function handleConcretar(reservaId) {
    if (!window.confirm('¿Confirmar que la adopción se concretó?')) return
    try {
      await concretarReserva(reservaId)
      cargarReservas()
      getMisAnimales().then(res => setAnimales(res.data)).catch(() => {})
    } catch (e) {
      alert(e.response?.data || 'Error al concretar.')
    }
  }

  function handleCancelarReserva(reservaId) {
    setCancelandoReservaId(reservaId)
  }

  async function confirmarCancelacion(reservaId, motivo) {
    setCancelandoReservaId(null)
    try {
      await cancelarReserva(reservaId, motivo)
      cargarReservas()
      getMisAnimales().then(res => setAnimales(res.data)).catch(() => {})
    } catch (e) {
      alert(e.response?.data || 'Error al cancelar.')
    }
  }

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

  async function handlePausar(id) {
    setModalPausarEliminarId(null)
    setError('')
    try {
      await pausarPublicacion(id)
      setAnimales(prev => prev.map(a => a.id === id ? { ...a, eliminado: true } : a))
      setReportes(prev => prev.map(r => r.id === id ? { ...r, eliminado: true } : r))
    } catch (err) {
      setError(err.response?.data || 'No se pudo pausar la publicación.')
    }
  }

  async function handleEliminarPermanente(id) {
    setModalPausarEliminarId(null)
    setError('')
    try {
      await eliminarPublicacionPermanente(id)
      setAnimales(prev => prev.filter(a => a.id !== id))
      setReportes(prev => prev.filter(r => r.id !== id))
    } catch (err) {
      setError(err.response?.data || 'No se pudo eliminar la publicación.')
    }
  }

  async function handleReactivar(id) {
    setError('')
    try {
      const res = await reactivarPublicacion(id)
      setAnimales(prev => prev.map(a => a.id === id ? res.data : a))
      setReportes(prev => prev.map(r => r.id === id ? res.data : r))
    } catch (err) {
      setError(err.response?.data || 'No se pudo reactivar la publicación.')
    }
  }

  async function handleEliminarFoto(animalId, fotoId) {
    if (!window.confirm('\u00bfEliminear esta foto?')) return
    setError('')
    try {
      await eliminarFotoAnimal(animalId, fotoId)
      const actualizar = prev => prev.map(a => a.id === animalId
        ? { ...a, fotos: a.fotos.map(f => f.id === fotoId ? { ...f, estado: 'ELIMINADA', motivoRechazo: null } : f) }
        : a
      )
      setAnimales(actualizar)
      setReportes(actualizar)
    } catch (err) {
      setError(err.response?.data || 'No se pudo eliminar la foto.')
    }
  }

  const enRevision = animales.filter(a => !a.eliminado && !a.aprobado && !a.rechazado)
  const enAdopcion = animales.filter(a => !a.eliminado && a.aprobado && a.estado === 'EN_ADOPCION')
  const reservados = reservas
  const adoptados = animales.filter(a => !a.eliminado && a.aprobado && a.estado === 'ADOPTADO')

  const perdidos = reportes.filter(r => !r.eliminado && r.estado === 'PERDIDO')
  const encontrados = reportes.filter(r => !r.eliminado && r.estado === 'ENCONTRADO')
  const resueltos = reportes.filter(r => !r.eliminado && r.estado === 'RESUELTO')

  const pausados = [
    ...animales.filter(a => a.eliminado && !a.eliminadoPermanente),
    ...reportes.filter(r => r.eliminado && !r.eliminadoPermanente),
  ]

  return (
    <div>
      <h2>Mis publicaciones</h2>

      {error && <p style={{ color: 'red' }}>{error}</p>}

      <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginBottom: '1rem' }}>
        {esRescatista && (
          <>
            <button onClick={() => setTab('en_revision')} disabled={tab === 'en_revision'}>
              En revision ({enRevision.length})
            </button>
            <button onClick={() => setTab('adopcion')} disabled={tab === 'adopcion'}>
              En adopcion ({enAdopcion.length})
            </button>
            <button onClick={() => setTab('reservados')} disabled={tab === 'reservados'}>
              Reservados ({reservados.length})
            </button>
            <button onClick={() => setTab('adoptados')} disabled={tab === 'adoptados'}>
              Adoptados ({adoptados.length})
            </button>
          </>
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
        <button onClick={() => setTab('pausados')} disabled={tab === 'pausados'}>
          Pausados ({pausados.length})
        </button>
      </div>

      {tab === 'en_revision' && esRescatista && (
        <div>
          <Link to="/agregar-animal">+ Publicar animal en adopcion</Link>
          <br /><br />
          {enRevision.length === 0 ? (
            <p>No tenes animales en revision.</p>
          ) : (
            enRevision.map(animal => (
              <div key={animal.id} style={{ border: '1px solid #ccc', margin: '1rem 0', padding: '1rem' }}>
                <h3>{animal.nombre} <span style={{ fontSize: '0.85rem', color: '#888' }}>[En revision]</span></h3>
                <StatsAnimal animal={animal} />
                <p>Tipo: {ETIQUETA_TIPO[animal.tipo]}</p>
                {animal.sexo && <p>Sexo: {ETIQUETA_SEXO[animal.sexo]}</p>}
                {animal.edad && <p>Edad: {ETIQUETA_EDAD[animal.edad]}</p>}
                {animal.tipoAdopcion && <p>Tipo de adopción: {animal.tipoAdopcion === 'PERMANENTE' ? 'Permanente' : 'Tránsito'}</p>}
                <p>Ubicación: {animal.ciudad}, {animal.provincia}</p>
                {(animal.amigableConGatos || animal.amigableConPerros || animal.amigableConNinos) && (
                  <p>Amigable con: {[animal.amigableConGatos && 'gatos', animal.amigableConPerros && 'perros', animal.amigableConNinos && 'niños'].filter(Boolean).join(', ')}</p>
                )}
                {animal.descripcion && <p>Descripción: {animal.descripcion}</p>}
                <FotosList fotos={animal.fotos} onEliminar={(fotoId) => handleEliminarFoto(animal.id, fotoId)} />
                <AgregarFotosBtn
                  id={animal.id}
                  rechazado={false}
                  fotosCount={animal.fotos.length}
                  fotosNuevas={fotosNuevas}
                  setFotosNuevas={setFotosNuevas}
                  onAgregar={handleAgregarFotosAnimal}
                />
                <div style={{ marginTop: '0.5rem' }}>
                  <button onClick={() => setModalPausarEliminarId(animal.id)} style={{ color: '#c00' }}>
                    Eliminar publicación
                  </button>
                </div>
              </div>
            ))
          )}
        </div>
      )}

      {tab === 'adopcion' && esRescatista && (
        <div>
          {enAdopcion.length === 0 ? (
            <p>No tenes animales en adopcion activos.</p>
          ) : (
            enAdopcion.map(animal => (
              <div key={animal.id} style={{ border: '1px solid #ccc', margin: '1rem 0', padding: '1rem' }}>
                <h3>{animal.nombre}</h3>
                <StatsAnimal animal={animal} />
                <p>Tipo: {ETIQUETA_TIPO[animal.tipo]}</p>
                {animal.sexo && <p>Sexo: {ETIQUETA_SEXO[animal.sexo]}</p>}
                {animal.edad && <p>Edad: {ETIQUETA_EDAD[animal.edad]}</p>}
                {animal.tipoAdopcion && <p>Tipo de adopción: {animal.tipoAdopcion === 'PERMANENTE' ? 'Permanente' : 'Tránsito'}</p>}
                <p>Ubicación: {animal.ciudad}, {animal.provincia}</p>
                {(animal.amigableConGatos || animal.amigableConPerros || animal.amigableConNinos) && (
                  <p>Amigable con: {[animal.amigableConGatos && 'gatos', animal.amigableConPerros && 'perros', animal.amigableConNinos && 'niños'].filter(Boolean).join(', ')}</p>
                )}
                {animal.descripcion && <p>Descripción: {animal.descripcion}</p>}
                <FotosList fotos={animal.fotos} onEliminar={(fotoId) => handleEliminarFoto(animal.id, fotoId)} />
                <AgregarFotosBtn
                  id={animal.id}
                  rechazado={false}
                  fotosCount={animal.fotos.length}
                  fotosNuevas={fotosNuevas}
                  setFotosNuevas={setFotosNuevas}
                  onAgregar={handleAgregarFotosAnimal}
                />
                <div style={{ marginTop: '0.5rem', display: 'flex', gap: 8 }}>
                  <button onClick={() => handleCambiarEstado(animal.id, 'ADOPTADO')}>
                    Marcar como adoptado
                  </button>
                  <button onClick={() => setModalPausarEliminarId(animal.id)} style={{ color: '#c00' }}>
                    Pausar / eliminar publicación
                  </button>
                </div>
              </div>
            ))
          )}
        </div>
      )}

      {tab === 'reservados' && esRescatista && (
        <div>
          {reservados.length === 0 ? (
            <p>No tenes animales reservados.</p>
          ) : (
            reservados.map(r => {
              const animal = animales.find(a => a.id === r.animalId)
              const activa = r.estado === 'ACTIVA'
              return (
                <div key={r.reservaId} style={{ border: `2px solid ${activa ? '#f0a500' : '#bbb'}`, borderRadius: 6, margin: '1rem 0', padding: '1rem', background: activa ? '#fffbf0' : '#fafafa', color: '#222' }}>
                  <h3>{r.animalNombre}</h3>
                  {animal && (
                    <>
                      <p>Tipo: {ETIQUETA_TIPO[animal.tipo]}</p>
                      {animal.sexo && <p>Sexo: {ETIQUETA_SEXO[animal.sexo]}</p>}
                      {animal.edad && <p>Edad: {ETIQUETA_EDAD[animal.edad]}</p>}
                      <p>Ubicación: {animal.ciudad}, {animal.provincia}</p>
                      <FotosList fotos={animal.fotos} />
                    </>
                  )}
                  <p style={{ margin: '8px 0', fontSize: 13, color: '#555' }}>
                    {activa
                      ? <>✅ Reservado para: <strong>{r.adoptanteNombre}</strong></>
                      : <>⏳ Propuesta enviada a <strong>{r.adoptanteNombre}</strong> — esperando confirmación</>}
                  </p>
                  {activa && (
                    <div style={{ display: 'flex', gap: 8 }}>
                      <button
                        onClick={() => handleConcretar(r.reservaId)}
                        style={{ fontSize: 13, background: '#2e7d32', color: '#fff', border: 'none', padding: '6px 14px', borderRadius: 4, cursor: 'pointer' }}
                      >
                        ✅ Adopción concretada
                      </button>
                      <button
                        onClick={() => handleCancelarReserva(r.reservaId)}
                        style={{ fontSize: 13, background: '#c62828', color: '#fff', border: 'none', padding: '6px 14px', borderRadius: 4, cursor: 'pointer' }}
                      >
                        ❌ Cancelar reserva
                      </button>
                    </div>
                  )}
                </div>
              )
            })
          )}
        </div>
      )}

      {tab === 'adoptados' && esRescatista && (
        <div>
          {adoptados.length === 0 ? (
            <p>Todavia no tenes animales adoptados.</p>
          ) : (
            adoptados.map(animal => (
              <div key={animal.id} style={{ border: '1px solid #ccc', margin: '1rem 0', padding: '1rem' }}>
                <h3>{animal.nombre}</h3>
                <StatsAnimal animal={animal} />
                <p>Tipo: {ETIQUETA_TIPO[animal.tipo]}</p>
                {animal.sexo && <p>Sexo: {ETIQUETA_SEXO[animal.sexo]}</p>}
                {animal.edad && <p>Edad: {ETIQUETA_EDAD[animal.edad]}</p>}
                {animal.tipoAdopcion && <p>Tipo de adopción: {animal.tipoAdopcion === 'PERMANENTE' ? 'Permanente' : 'Tránsito'}</p>}
                <p>Ubicación: {animal.ciudad}, {animal.provincia}</p>
                {animal.descripcion && <p>Descripción: {animal.descripcion}</p>}
                <FotosList fotos={animal.fotos} />
              </div>
            ))
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
                  {reporte.fechaAvistamiento && <p>Fecha: {reporte.fechaAvistamiento}</p>}
                  {reporte.estado === 'ENCONTRADO' && <p>En posesión del publicador: {reporte.enPosesionDelPublicador ? 'Sí' : 'No'}</p>}
                  {reporte.descripcion && <p>Descripción: {reporte.descripcion}</p>}
                  <FotosList fotos={reporte.fotos} onEliminar={!resuelto ? (fotoId) => handleEliminarFoto(reporte.id, fotoId) : undefined} />
                  <AgregarFotosBtn
                    id={reporte.id}
                    rechazado={reporte.rechazado || resuelto}
                    fotosCount={reporte.fotos.length}
                    fotosNuevas={fotosNuevas}
                    setFotosNuevas={setFotosNuevas}
                    onAgregar={handleAgregarFotosReporte}
                  />
                  <div style={{ marginTop: '0.5rem', display: 'flex', gap: 8 }}>
                    {reporte.aprobado && !resuelto && (
                      <button onClick={() => handleResolver(reporte.id)}>
                        Marcar como resuelto
                      </button>
                    )}
                    {!resuelto && (
                      <button onClick={() => setModalPausarEliminarId(reporte.id)} style={{ color: '#c00' }}>
                        Pausar / eliminar publicación
                      </button>
                    )}
                  </div>
                </div>
              )
            })
          })()}
        </div>
      )}

      {tab === 'pausados' && (
        <div>
          {pausados.length === 0 ? (
            <p>No tenés publicaciones pausadas.</p>
          ) : (
            pausados.map(item => {
              const esReporte = item.categoria === 'PERDIDO_ENCONTRADO'
              const titulo = esReporte ? ETIQUETA_TIPO[item.tipo] : item.nombre
              return (
                <div
                  key={item.id}
                  style={{ border: '1px solid #ccc', margin: '1rem 0', padding: '1rem', opacity: 0.7 }}
                >
                  <h3>{titulo}</h3>
                  {item.eliminadoPorAdmin ? (
                    <p style={{ color: '#c00' }}>
                      Eliminada por un administrador
                      {item.motivoEliminacion && `: ${item.motivoEliminacion}`}
                    </p>
                  ) : (
                    <p style={{ color: '#888' }}>Pausada por vos</p>
                  )}
                  {item.fotos?.length > 0 && (
                    <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap', margin: '0.5rem 0' }}>
                      {item.fotos.slice(0, 1).map(f => (
                        <img key={f.id} src={f.url} alt="foto" style={{ width: 80, height: 80, objectFit: 'cover' }} />
                      ))}
                    </div>
                  )}
                  {!item.eliminadoPorAdmin && (
                    <div style={{ marginTop: '0.5rem' }}>
                      <button onClick={() => handleReactivar(item.id)}>
                        Reactivar
                      </button>
                    </div>
                  )}
                </div>
              )
            })
          )}
        </div>
      )}

      <br />
      <Link to="/">Volver al inicio</Link>

      {modalPausarEliminarId && (() => {
        const animalModal = animales.find(a => a.id === modalPausarEliminarId);
        const enRevisionModal = animalModal && !animalModal.aprobado && !animalModal.rechazado;
        return (
          <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000 }}>
            <div style={{ background: '#fff', color: '#222', padding: '2rem', borderRadius: 8, width: 400, maxWidth: '90%' }}>
              <h3 style={{ marginTop: 0 }}>{enRevisionModal ? '¿Eliminar publicación?' : '¿Deseas pausar o eliminar?'}</h3>
              <p style={{ fontSize: 14, color: '#555', marginBottom: 16 }}>
                {enRevisionModal
                  ? 'Esta publicación está pendiente de revisión. Si la eliminás, no podrás recuperarla.'
                  : <>Si planeás reactivar la publicación más adelante, elegí <strong>Pausar</strong>. Si elegís <strong>Eliminar</strong>, la publicación se borra de forma permanente y no podrás recuperarla.</>
                }
              </p>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
                {!enRevisionModal && (
                  <button onClick={() => handlePausar(modalPausarEliminarId)}
                    style={{ padding: '10px 14px', textAlign: 'left', background: '#fff3e0', border: '1px solid #f0a500', borderRadius: 4, cursor: 'pointer', fontSize: 13 }}>
                    <strong>Pausar publicación</strong>
                    <div style={{ fontSize: 11, color: '#888', marginTop: 3 }}>Dejará de verse en la plataforma, pero podés reactivarla después.</div>
                  </button>
                )}
                <button onClick={() => handleEliminarPermanente(modalPausarEliminarId)}
                  style={{ padding: '10px 14px', textAlign: 'left', background: '#ffebee', border: '1px solid #c62828', borderRadius: 4, cursor: 'pointer', fontSize: 13 }}>
                  <strong>Eliminar permanentemente</strong>
                  <div style={{ fontSize: 11, color: '#888', marginTop: 3 }}>Esta acción no se puede deshacer.</div>
                </button>
                <button onClick={() => setModalPausarEliminarId(null)} style={{ marginTop: 4, fontSize: 13 }}>
                  Volver
                </button>
              </div>
            </div>
          </div>
        );
      })()}

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
              <button onClick={() => setCancelandoReservaId(null)} style={{ marginTop: 4, fontSize: 13 }}>
                Volver
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

export default MisPublicaciones
