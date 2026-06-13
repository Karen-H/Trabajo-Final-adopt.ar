import { useState, useEffect } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { getEncontrados } from '../api/reporte'
import { getFavoritos, agregarFavorito, quitarFavorito } from '../api/favorito'
import ModalDenuncia from '../components/ModalDenuncia'
import FiltroUbicacion from '../components/FiltroUbicacion'
import Paginacion from '../components/Paginacion'
import { useAuth } from '../context/AuthContext'

const TIPOS = ['PERRO', 'GATO', 'OTRO']
const ETIQUETA_TIPO = { PERRO: 'Perro', GATO: 'Gato', OTRO: 'Otro' }

function filtrar(reportes, tipos, provincia, ciudad) {
  return reportes.filter(r => {
    const pasaTipo = tipos.length === 0 || tipos.includes(r.tipo)
    const pasaProv = !provincia || (r.provincia || '') === provincia
    const pasaCiudad = !ciudad || (r.ciudad || '') === ciudad
    return pasaTipo && pasaProv && pasaCiudad
  })
}

function Encontrados() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const estaLogueado = !!localStorage.getItem('token')
  const [reportes, setReportes] = useState([])
  const [cargando, setCargando] = useState(true)
  const [tipos, setTipos] = useState([])
  const [provincia, setProvincia] = useState('')
  const [ciudad, setCiudad] = useState('')
  const [pagina, setPagina] = useState(1)
  const [favoritos, setFavoritos] = useState(new Set())
  const [denunciados, setDenunciados] = useState(new Set())
  const [denunciaModal, setDenunciaModal] = useState(null)

  useEffect(() => {
    getEncontrados()
      .then(r => setReportes(r.data))
      .catch(() => {})
      .finally(() => setCargando(false))
  }, [])

  function toggleTipo(tipo) {
    setTipos(prev => prev.includes(tipo) ? prev.filter(t => t !== tipo) : [...prev, tipo])
  }

  const hayFiltros = tipos.length > 0 || provincia || ciudad
  const visibles = filtrar(reportes, tipos, provincia, ciudad)

  useEffect(() => { setPagina(1) }, [tipos, provincia, ciudad])

  useEffect(() => {
    if (!estaLogueado) return
    getFavoritos()
      .then(r => setFavoritos(new Set(r.data.map(f => f.animalId))))
      .catch(() => {})
  }, [estaLogueado])

  function toggleFavorito(id) {
    if (!estaLogueado) {
      localStorage.setItem('pendingFavorito', id)
      navigate('/login')
      return
    }
    if (favoritos.has(id)) {
      quitarFavorito(id)
        .then(() => setFavoritos(prev => { const n = new Set(prev); n.delete(id); return n }))
        .catch(() => {})
    } else {
      agregarFavorito(id)
        .then(() => setFavoritos(prev => new Set([...prev, id])))
        .catch(() => {})
    }
  }

  const POR_PAGINA = 10
  const paginados = visibles.slice((pagina - 1) * POR_PAGINA, pagina * POR_PAGINA)

  if (cargando) return <p>Cargando...</p>

  return (
    <div>
      <h2>Animales encontrados</h2>

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
          animales={reportes}
          provincia={provincia}
          ciudad={ciudad}
          onProvinciaChange={setProvincia}
          onCiudadChange={setCiudad}
        />
        {hayFiltros && (
          <button onClick={() => { setTipos([]); setProvincia(''); setCiudad('') }}>
            Limpiar filtros
          </button>
        )}
      </div>

      {reportes.length === 0 ? (
        <p>No hay reportes de animales encontrados en este momento.</p>
      ) : visibles.length === 0 ? (
        <p>No hay resultados para los filtros seleccionados.</p>
      ) : (
        <>
          {paginados.map(r => (
            <div key={r.id} style={{ border: '1px solid #ccc', marginBottom: 12 }}>
              <Link to={`/animal/${r.id}`} style={{ display: 'block', padding: 12, color: 'inherit', textDecoration: 'none' }}>
                {r.fotos?.length > 0 && (
                  <img src={r.fotos[0].url} alt="foto" style={{ width: 120, height: 120, objectFit: 'cover', display: 'block', marginBottom: 8 }} />
                )}
                <p style={{ margin: '2px 0', fontWeight: 600 }}>{r.nombre || ETIQUETA_TIPO[r.tipo]}</p>
                {(r.ciudad || r.provincia) && (
                  <p style={{ margin: '2px 0', fontSize: 12, color: '#666' }}>{[r.ciudad, r.provincia].filter(Boolean).join(', ')}</p>
                )}
              </Link>
              <div style={{ padding: '0 12px 8px', display: 'flex', gap: 8, alignItems: 'center' }}>
                <button
                  onClick={() => toggleFavorito(r.id)}
                  style={{ background: 'none', border: 'none', cursor: 'pointer', fontSize: 18, padding: 0 }}
                  title={favoritos.has(r.id) ? 'Quitar de favoritos' : 'Agregar a favoritos'}
                >
                  {favoritos.has(r.id) ? '❤️' : '🤍'}
                </button>
                {estaLogueado && user?.id !== r.usuarioId && (
                  <button
                    onClick={() => denunciados.has(r.id) ? alert('Ya denunciaste esta publicación') : setDenunciaModal(r.id)}
                    style={{ background: 'none', border: 'none', cursor: 'pointer', fontSize: 12, color: '#999', padding: 0 }}
                  >
                    🚩 Reportar
                  </button>
                )}
              </div>
            </div>
          ))}
          <Paginacion total={visibles.length} porPagina={POR_PAGINA} pagina={pagina} onChange={setPagina} />
        </>
      )}
      {denunciaModal !== null && (
        <ModalDenuncia
          animalId={denunciaModal}
          onClose={() => setDenunciaModal(null)}
          onSuccess={() => {
            setDenunciados(prev => new Set([...prev, denunciaModal]))
            setDenunciaModal(null)
            alert('Denuncia enviada. El administrador la revisará.')
          }}
        />
      )}
    </div>
  )
}

export default Encontrados
