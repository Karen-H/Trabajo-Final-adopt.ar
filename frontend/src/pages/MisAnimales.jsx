import { useState, useEffect } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { getMisAnimales, cambiarEstadoAnimal, agregarFotosAnimal } from '../api/animal'

const ESTADOS = ['EN_ADOPCION', 'ADOPTADO', 'PERDIDO', 'ENCONTRADO']

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
  }, [navigate])

  async function handleCambiarEstado(id, estado) {
    try {
      const res = await cambiarEstadoAnimal(id, estado)
      setAnimales(prev => prev.map(a => a.id === id ? res.data : a))
    } catch {
      setError('No se pudo cambiar el estado.')
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

              <p>
                {animal.tipo.charAt(0) + animal.tipo.slice(1).toLowerCase()}
                {' - '}
                {animal.sexo.charAt(0) + animal.sexo.slice(1).toLowerCase()}
                {' - '}
                {ETIQUETA_EDAD[animal.edad]}
              </p>
              <p>{animal.ciudad}, {animal.provincia}</p>
              <p>Adopcion: {animal.tipoAdopcion === 'PERMANENTE' ? 'Permanente' : 'Transito'}</p>
              <p>
                Amigable con:{' '}
                {[
                  animal.amigableConGatos && 'gatos',
                  animal.amigableConPerros && 'perros',
                  animal.amigableConNinos && 'ninos',
                ].filter(Boolean).join(', ') || 'ninguna especie indicada'}
              </p>
              {animal.descripcion && <p>{animal.descripcion}</p>}

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
    </div>
  )
}

export default MisAnimales