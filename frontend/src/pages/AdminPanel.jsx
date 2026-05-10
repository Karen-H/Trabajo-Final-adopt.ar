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
    </div>
  )
}

export default AdminPanel
