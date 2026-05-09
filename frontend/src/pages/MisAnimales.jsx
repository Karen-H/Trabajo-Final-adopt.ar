import { useState, useEffect } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { getMisAnimales, cambiarEstadoAnimal } from '../api/animal'

const ESTADOS = ['EN_ADOPCION', 'ADOPTADO', 'PERDIDO', 'ENCONTRADO']

const ETIQUETA_ESTADO = {
  EN_ADOPCION: 'En adopción',
  ADOPTADO: 'Adoptado',
  PERDIDO: 'Perdido',
  ENCONTRADO: 'Encontrado',
}

const ETIQUETA_EDAD = {
  CACHORRO: 'Cachorro (0-6 meses)',
  JOVEN: 'Joven (6 meses - 2 años)',
  ADULTO: 'Adulto (2-7 años)',
  SENIOR: 'Senior (7+ años)',
}

function MisAnimales() {
  const navigate = useNavigate()
  const [animales, setAnimales] = useState([])
  const [error, setError] = useState('')

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

  return (
    <div>
      <h2>Mis animales</h2>
      <Link to="/agregar-animal">+ Publicar animal</Link>

      {error && <p>{error}</p>}

      {animales.length === 0 ? (
        <p>Todavía no publicaste ningún animal.</p>
      ) : (
        animales.map(animal => (
          <div key={animal.id} style={{ border: '1px solid #ccc', margin: '1rem 0', padding: '1rem' }}>
            <h3>{animal.nombre}</h3>
            <p>
              {animal.tipo.charAt(0) + animal.tipo.slice(1).toLowerCase()}
              {' · '}
              {animal.sexo.charAt(0) + animal.sexo.slice(1).toLowerCase()}
              {' · '}
              {ETIQUETA_EDAD[animal.edad]}
            </p>
            <p>
              {animal.ciudad}, {animal.provincia}
            </p>
            <p>
              Adopción: {animal.tipoAdopcion === 'PERMANENTE' ? 'Permanente' : 'Tránsito'}
            </p>
            <p>
              Amigable con:{' '}
              {[
                animal.amigableConGatos && 'gatos',
                animal.amigableConPerros && 'perros',
                animal.amigableConNinos && 'niños',
              ].filter(Boolean).join(', ') || 'ninguna especie indicada'}
            </p>
            {animal.descripcion && <p>{animal.descripcion}</p>}

            <div>
              {animal.fotos.map(foto => (
                <img
                  key={foto.id}
                  src={foto.url}
                  alt={animal.nombre}
                  style={{ width: 120, height: 120, objectFit: 'cover', marginRight: 8 }}
                />
              ))}
            </div>

            <div>
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
          </div>
        ))
      )}

      <br />
      <Link to="/">Volver al inicio</Link>
    </div>
  )
}

export default MisAnimales
