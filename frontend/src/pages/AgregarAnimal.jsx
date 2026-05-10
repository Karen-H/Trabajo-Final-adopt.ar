import { useState, useEffect } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { getProfile } from '../api/user'
import { crearAnimal } from '../api/animal'
import { buscarNominatim } from '../api/reporte'

const TIPOS = ['PERRO', 'GATO', 'OTRO']
const SEXOS = ['MACHO', 'HEMBRA']
const EDADES = [
  { value: 'CACHORRO', label: 'Cachorro (0-6 meses)' },
  { value: 'JOVEN', label: 'Joven (6 meses - 2 años)' },
  { value: 'ADULTO', label: 'Adulto (2-7 años)' },
  { value: 'SENIOR', label: 'Senior (7+ años)' },
]
const TIPOS_ADOPCION = [
  { value: 'PERMANENTE', label: 'Permanente' },
  { value: 'TRANSITO', label: 'Tránsito' },
]

function AgregarAnimal() {
  const navigate = useNavigate()
  const [perfil, setPerfil] = useState(null)
  const [form, setForm] = useState({
    nombre: '',
    sexo: '',
    edad: '',
    tipo: '',
    tipoAdopcion: '',
    amigableConGatos: false,
    amigableConPerros: false,
    amigableConNinos: false,
    descripcion: '',
    direccion: '',
    latitud: '',
    longitud: '',
  })
  const [fotos, setFotos] = useState([])
  const [error, setError] = useState('')
  const [cargando, setCargando] = useState(false)
  const [geocodificando, setGeocodificando] = useState(false)

  useEffect(() => {
    if (!localStorage.getItem('token')) {
      navigate('/login')
      return
    }
    getProfile()
      .then(async res => {
        const p = res.data
        setPerfil(p)
        if (p.ciudad && p.provincia) {
          setGeocodificando(true)
          try {
            const resultados = await buscarNominatim(`${p.ciudad}, ${p.provincia}, Argentina`)
            if (resultados.length > 0) {
              const r = resultados[0]
              setForm(f => ({
                ...f,
                direccion: `${p.ciudad}, ${p.provincia}`,
                latitud: r.lat,
                longitud: r.lon,
              }))
            }
          } catch {
            // geocoding fallo, se muestra el campo sin coordenadas
          } finally {
            setGeocodificando(false)
          }
        }
      })
      .catch(() => navigate('/login'))
  }, [navigate])

  function handleChange(e) {
    const { name, value, type, checked } = e.target
    setForm({ ...form, [name]: type === 'checkbox' ? checked : value })
  }

  function handleFotos(e) {
    const archivos = Array.from(e.target.files)
    if (archivos.length < 1 || archivos.length > 5) {
      setError('Debés seleccionar entre 1 y 5 fotos')
      return
    }
    setError('')
    setFotos(archivos)
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')

    if (!form.latitud || !form.longitud) {
      setError('No se pudo obtener las coordenadas de tu ubicación. Verificá tu provincia y ciudad en el perfil.')
      return
    }
    if (fotos.length === 0) {
      setError('Debés subir al menos una foto')
      return
    }

    const formData = new FormData()
    Object.entries(form).forEach(([key, value]) => {
      if (value !== '') formData.append(key, value)
    })
    fotos.forEach(foto => formData.append('fotos', foto))

    setCargando(true)
    try {
      await crearAnimal(formData)
      navigate('/mis-publicaciones')
    } catch (err) {
      const msg = err.response?.data
      setError(typeof msg === 'string' ? msg : 'Ocurrió un error. Intentá de nuevo.')
    } finally {
      setCargando(false)
    }
  }

  if (!perfil) return null

  const sinUbicacion = !perfil.provincia || !perfil.ciudad

  return (
    <div>
      <h2>Publicar animal</h2>

      {sinUbicacion && (
        <p>
          Necesitás configurar tu provincia y ciudad en{' '}
          <Link to="/perfil">tu perfil</Link> antes de publicar un animal.
        </p>
      )}

      <form onSubmit={handleSubmit}>
        <div>
          <label>Nombre</label><br />
          <input name="nombre" value={form.nombre} onChange={handleChange} required disabled={sinUbicacion} />
        </div>

        <div>
          <label>Tipo</label><br />
          <select name="tipo" value={form.tipo} onChange={handleChange} required disabled={sinUbicacion}>
            <option value="">Seleccioná</option>
            {TIPOS.map(t => <option key={t} value={t}>{t.charAt(0) + t.slice(1).toLowerCase()}</option>)}
          </select>
        </div>

        <div>
          <label>Sexo</label><br />
          <select name="sexo" value={form.sexo} onChange={handleChange} required disabled={sinUbicacion}>
            <option value="">Seleccioná</option>
            {SEXOS.map(s => <option key={s} value={s}>{s.charAt(0) + s.slice(1).toLowerCase()}</option>)}
          </select>
        </div>

        <div>
          <label>Edad</label><br />
          <select name="edad" value={form.edad} onChange={handleChange} required disabled={sinUbicacion}>
            <option value="">Seleccioná</option>
            {EDADES.map(e => <option key={e.value} value={e.value}>{e.label}</option>)}
          </select>
        </div>

        <div>
          <label>Tipo de adopción</label><br />
          <select name="tipoAdopcion" value={form.tipoAdopcion} onChange={handleChange} required disabled={sinUbicacion}>
            <option value="">Seleccioná</option>
            {TIPOS_ADOPCION.map(t => <option key={t.value} value={t.value}>{t.label}</option>)}
          </select>
        </div>

        <div>
          <label>Dirección</label><br />
          <input
            type="text"
            value={geocodificando ? 'Obteniendo ubicación...' : form.direccion}
            readOnly
            disabled
          />
        </div>

        <div>
          <label>¿Es amigable con...?</label><br />
          <label>
            <input type="checkbox" name="amigableConGatos" checked={form.amigableConGatos} onChange={handleChange} disabled={sinUbicacion} />
            {' '}Gatos
          </label>
          {'  '}
          <label>
            <input type="checkbox" name="amigableConPerros" checked={form.amigableConPerros} onChange={handleChange} disabled={sinUbicacion} />
            {' '}Perros
          </label>
          {'  '}
          <label>
            <input type="checkbox" name="amigableConNinos" checked={form.amigableConNinos} onChange={handleChange} disabled={sinUbicacion} />
            {' '}Niños
          </label>
        </div>

        <div>
          <label>Descripción (opcional)</label><br />
          <textarea name="descripcion" value={form.descripcion} onChange={handleChange} rows={4} disabled={sinUbicacion} />
        </div>

        <div>
          <label>Fotos (mínimo 1, máximo 5)</label><br />
          <input type="file" accept="image/*" multiple onChange={handleFotos} disabled={sinUbicacion} />
          {fotos.length > 0 && <span> {fotos.length} foto(s) seleccionada(s)</span>}
        </div>

        {error && <p>{error}</p>}

        <button type="submit" disabled={sinUbicacion || cargando}>
          {cargando ? 'Publicando...' : 'Publicar animal'}
        </button>
      </form>

      <br />
      <Link to="/mis-animales">Volver a mis animales</Link>
    </div>
  )
}

export default AgregarAnimal
