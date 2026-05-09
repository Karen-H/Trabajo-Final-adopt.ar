import { useState, useEffect } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import {
  getAnimalesPendientes, aprobarAnimal, rechazarAnimal,
  getFotosPendientes, aprobarFoto, rechazarFoto,
} from '../api/admin'

const ETIQUETA_TIPO = { PERRO: 'Perro', GATO: 'Gato', OTRO: 'Otro' }
const ETIQUETA_SEXO = { MACHO: 'Macho', HEMBRA: 'Hembra' }
const ETIQUETA_EDAD = {
  CACHORRO: 'Cachorro (0-6 meses)',
  JOVEN: 'Joven (6 meses - 2 años)',
  ADULTO: 'Adulto (2-7 años)',
  SENIOR: 'Senior (7+ años)',
}

function AdminPanel() {
  const navigate = useNavigate()
  const [tab, setTab] = useState('animales')
  const [animales, setAnimales] = useState([])
  const [fotos, setFotos] = useState([])
  const [error, setError] = useState('')
  const [motivoAnimal, setMotivoAnimal] = useState({})
  const [motivoFoto, setMotivoFoto] = useState({})

  useEffect(() => {
    if (localStorage.getItem('role') !== 'ADMIN') {
      navigate('/')
      return
    }
    cargarAnimales()
    cargarFotos()
  }, [navigate])

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
      setError('Ingresá un motivo de rechazo antes de rechazar.')
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
      setError('Ingresá un motivo de rechazo antes de rechazar.')
      return
    }
    try {
      await rechazarFoto(id, motivo)
      setFotos(prev => prev.filter(f => f.id !== id))
    } catch {
      setError('No se pudo rechazar la foto.')
    }
  }

  return (
    <div>
      <h2>Panel de administración</h2>

      {error && <p style={{ color: 'red' }}>{error}</p>}

      <div>
        <button onClick={() => setTab('animales')} disabled={tab === 'animales'}>
          Animales pendientes ({animales.length})
        </button>
        {' '}
        <button onClick={() => setTab('fotos')} disabled={tab === 'fotos'}>
          Fotos pendientes ({fotos.length})
        </button>
      </div>

      <br />

      {tab === 'animales' && (
        <div>
          {animales.length === 0 ? (
            <p>No hay animales pendientes de aprobación.</p>
          ) : (
            animales.map(animal => (
              <div key={animal.id} style={{ border: '1px solid #ccc', margin: '1rem 0', padding: '1rem' }}>
                <h3>{animal.nombre} — {ETIQUETA_TIPO[animal.tipo]}</h3>
                <p>
                  {ETIQUETA_SEXO[animal.sexo]}
                  {' · '}
                  {ETIQUETA_EDAD[animal.edad]}
                  {' · '}
                  Adopción {animal.tipoAdopcion === 'PERMANENTE' ? 'permanente' : 'en tránsito'}
                </p>
                <p>{animal.ciudad}, {animal.provincia}</p>
                <p>
                  Amigable con:{' '}
                  {[
                    animal.amigableConGatos && 'gatos',
                    animal.amigableConPerros && 'perros',
                    animal.amigableConNinos && 'niños',
                  ].filter(Boolean).join(', ') || 'ninguna especie indicada'}
                </p>
                {animal.descripcion && <p>{animal.descripcion}</p>}
                <p>Rescatista: {animal.rescatistaNombre}</p>

                <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', margin: '0.5rem 0' }}>
                  {animal.fotos.map(foto => (
                    <img
                      key={foto.id}
                      src={foto.url}
                      alt={animal.nombre}
                      style={{ width: 150, height: 150, objectFit: 'cover' }}
                    />
                  ))}
                </div>

                <div style={{ marginTop: '0.75rem', display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
                  <button onClick={() => handleAprobarAnimal(animal.id)}>Aprobar</button>
                  <input
                    type="text"
                    placeholder="Motivo de rechazo"
                    value={motivoAnimal[animal.id] || ''}
                    onChange={e => setMotivoAnimal(prev => ({ ...prev, [animal.id]: e.target.value }))}
                    style={{ width: 280 }}
                  />
                  <button onClick={() => handleRechazarAnimal(animal.id)}>Rechazar</button>
                </div>
              </div>
            ))
          )}
        </div>
      )}

      {tab === 'fotos' && (
        <div>
          {fotos.length === 0 ? (
            <p>No hay fotos pendientes de aprobación.</p>
          ) : (
            fotos.map(foto => (
              <div
                key={foto.id}
                style={{ border: '1px solid #ccc', margin: '1rem 0', padding: '1rem', display: 'flex', gap: '1rem', alignItems: 'flex-start' }}
              >
                <img
                  src={foto.url}
                  alt={foto.animalNombre}
                  style={{ width: 150, height: 150, objectFit: 'cover', flexShrink: 0 }}
                />
                <div>
                  <p>
                    <strong>{foto.animalNombre}</strong>
                    {' '}({ETIQUETA_TIPO[foto.animalTipo]})
                  </p>
                  <p>Rescatista: {foto.rescatistaNombre}</p>
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
            ))
          )}
        </div>
      )}

      <br />
      <Link to="/">Volver al inicio</Link>
    </div>
  )
}

export default AdminPanel
