import { useState, useEffect } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { getMisAnimales, cambiarEstadoAnimal, agregarFotosAnimal } from '../api/animal'
import { getMisReservasActivas, concretarReserva, cancelarReserva } from '../api/reserva'

const ESTADOS = ['EN_ADOPCION', 'ADOPTADO']

const ETIQUETA_TIPO = { PERRO: 'Perro', GATO: 'Gato', OTRO: 'Otro' }
const ETIQUETA_SEXO = { MACHO: 'Macho', HEMBRA: 'Hembra' }

const ETIQUETA_ESTADO = {
  EN_ADOPCION: 'En adopcion',
  ADOPTADO: 'Adoptado',
  PERDIDO: 'Perdido',
  ENCONTRADO: 'Encontrado',
}

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

function MisAnimales() {
  const navigate = useNavigate()
  const [animales, setAnimales] = useState([])
  const [reservasActivas, setReservasActivas] = useState([])
  const [cancelandoReservaId, setCancelandoReservaId] = useState(null)
  const [error, setError] = useState('')
  const [fotosNuevas, setFotosNuevas] = useState({})

  useEffect(() => {
    if (!localStorage.getItem('token')) {
      navigate('/login')
      return
    }
    getMisAnimales()
      .then(res => setAnimales(res.data))
      .catch(() => setError('No se pudieron cargar tus animales.'))
    cargarReservas()
  }, [navigate])

  function cargarReservas() {
    getMisReservasActivas()
      .then(res => setReservasActivas(res.data))
      .catch(() => {})
  }

  async function handleCambiarEstado(id, estado) {
    try {
      const res = await cambiarEstadoAnimal(id, estado)
      setAnimales(prev => prev.map(a => a.id === id ? res.data : a))
    } catch {
      setError('No se pudo cambiar el estado.')
    }
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

  function handleCancelar(reservaId) {
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

  async function handleAgregarFotos(animalId) {
    setError('')
    const files = fotosNuevas[animalId]
    if (!files || files.length === 0) return
    const formData = new FormData()
    Array.from(files).forEach(f => formData.append('fotos', f))
    try {
      const res = await agregarFotosAnimal(animalId, formData)
      setAnimales(prev => prev.map(a => a.id === animalId ? res.data : a))
      setFotosNuevas(prev => ({ ...prev, [animalId]: null }))
    } catch (err) {
      setError(err.response?.data || 'No se pudieron agregar las fotos.')
    }
  }

  function estadoRevision(animal) {
    if (animal.rechazado) return { texto: 'Rechazado', color: '#c00' }
    if (animal.aprobado) return { texto: 'Aprobado', color: '#080' }
    return { texto: 'En revision', color: '#888' }
  }

  return (
    <div>
      <h2>Mis animales</h2>
      <Link to="/agregar-animal">+ Publicar animal</Link>

      {error && <p style={{ color: 'red' }}>{error}</p>}

      {/* sección de reservados */}
      {reservasActivas.length > 0 && (
        <div style={{ margin: '1.5rem 0' }}>
          <h3 style={{ color: '#c47f00' }}>Animales con reserva</h3>
          {reservasActivas.map(r => {
            const activa = r.estado === 'ACTIVA'
            return (
              <div key={r.reservaId} style={{ border: `2px solid ${activa ? '#f0a500' : '#bbb'}`, borderRadius: 6, margin: '0.75rem 0', padding: '1rem', background: activa ? '#fffbf0' : '#fafafa' }}>
                <p style={{ margin: '0 0 4px', fontWeight: 600 }}>{r.animalNombre}</p>
                <p style={{ margin: '0 0 8px', fontSize: 13, color: '#555' }}>
                  {activa
                    ? <>Reservado para: <strong>{r.adoptanteNombre}</strong></>
                    : <>Propuesta enviada a <strong>{r.adoptanteNombre}</strong>, esperando confirmación</>}
                </p>
                {activa && (
                  <div style={{ display: 'flex', gap: 8 }}>
                    <button
                      onClick={() => handleConcretar(r.reservaId)}
                      style={{ fontSize: 13, background: '#2e7d32', color: '#fff', border: 'none', padding: '6px 14px', borderRadius: 4, cursor: 'pointer' }}
                    >
                      Adopción concretada
                    </button>
                    <button
                      onClick={() => handleCancelar(r.reservaId)}
                      style={{ fontSize: 13, background: '#c62828', color: '#fff', border: 'none', padding: '6px 14px', borderRadius: 4, cursor: 'pointer' }}
                    >
                      Cancelar reserva
                    </button>
                  </div>
                )}
              </div>
            )
          })}
        </div>
      )}

      {animales.length === 0 ? (
        <p>Todavia no publicaste ningun animal.</p>
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
                <p style={{ color: '#c00' }}>
                  Motivo de rechazo: {animal.motivoRechazo}
                </p>
              )}

              <p>Tipo: {ETIQUETA_TIPO[animal.tipo]}</p>
              {animal.sexo && <p>Sexo: {ETIQUETA_SEXO[animal.sexo]}</p>}
              {animal.edad && <p>Edad: {ETIQUETA_EDAD[animal.edad]}</p>}
              {animal.tipoAdopcion && <p>Tipo de adopción: {animal.tipoAdopcion === 'PERMANENTE' ? 'Permanente' : 'Tránsito'}</p>}
              <p>Ubicación: {animal.ciudad}, {animal.provincia}</p>
              {(animal.amigableConGatos || animal.amigableConPerros || animal.amigableConNinos) && (
                <p>Amigable con: {[animal.amigableConGatos && 'gatos', animal.amigableConPerros && 'perros', animal.amigableConNinos && 'niños'].filter(Boolean).join(', ')}</p>
              )}
              {animal.descripcion && <p>Descripción: {animal.descripcion}</p>}

              <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', margin: '0.5rem 0' }}>
                {animal.fotos.map(foto => (
                  <div key={foto.id} style={{ textAlign: 'center' }}>
                    <img
                      src={foto.url}
                      alt={animal.nombre}
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

              {!animal.rechazado && animal.fotos.length < 5 && (
                <div style={{ margin: '0.5rem 0' }}>
                  <input
                    type="file"
                    accept="image/*"
                    multiple
                    onChange={e => setFotosNuevas(prev => ({ ...prev, [animal.id]: e.target.files }))}
                  />
                  <button
                    onClick={() => handleAgregarFotos(animal.id)}
                    disabled={!fotosNuevas[animal.id] || fotosNuevas[animal.id].length === 0}
                    style={{ marginLeft: 8 }}
                  >
                    Agregar fotos
                  </button>
                </div>
              )}

              {animal.aprobado && (
                <div style={{ marginTop: '0.5rem' }}>
                  <strong>Estado: {ETIQUETA_ESTADO[animal.estado]}</strong>
                  {'  '}
                  <select
                    value={animal.estado}
                    onChange={e => handleCambiarEstado(animal.id, e.target.value)}
                  >
                    {ESTADOS.map(s => (
                      <option key={s} value={s}>{ETIQUETA_ESTADO[s]}</option>
                    ))}
                  </select>
                </div>
              )}
            </div>
          )
        })
      )}

      <br />
      <Link to="/">Volver al inicio</Link>

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

export default MisAnimales