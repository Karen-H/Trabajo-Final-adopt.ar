import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import {
  getMisItems,
  crearItem,
  editarItem,
  agregarFotosItem,
  eliminarFotoItem,
  eliminarItem,
} from '../api/item'
import { formatMonto } from '../utils/formatMonto'

const TIPOS_ITEM = [
  { value: 'INDUMENTARIA', label: 'Indumentaria' },
  { value: 'ACCESORIO', label: 'Accesorio' },
  { value: 'ALIMENTO', label: 'Alimento' },
  { value: 'JUGUETE', label: 'Juguete' },
  { value: 'HIGIENE', label: 'Higiene' },
  { value: 'CAMA', label: 'Cama' },
  { value: 'TRANSPORTE', label: 'Transporte' },
  { value: 'OTRO', label: 'Otro' },
]

const formInicial = { titulo: '', tipo: '', descripcion: '', precio: '', stock: '' }

function MiTienda() {
  const { user } = useAuth()
  const navigate = useNavigate()

  const [items, setItems] = useState([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState('')

  // crear
  const [mostrarFormCrear, setMostrarFormCrear] = useState(false)
  const [formCrear, setFormCrear] = useState(formInicial)
  const [fotosCrear, setFotosCrear] = useState([])
  const [creando, setCreando] = useState(false)
  const [errorCrear, setErrorCrear] = useState('')

  // editar
  const [editandoId, setEditandoId] = useState(null)
  const [formEditar, setFormEditar] = useState(formInicial)
  const [editando, setEditando] = useState(false)
  const [errorEditar, setErrorEditar] = useState('')

  // agregar fotos
  const [agregandoFotosId, setAgregandoFotosId] = useState(null)
  const [fotosNuevas, setFotosNuevas] = useState([])
  const [agregandoFotos, setAgregandoFotos] = useState(false)
  const [errorFotos, setErrorFotos] = useState('')

  useEffect(() => {
    if (!localStorage.getItem('token')) {
      navigate('/login')
      return
    }
    if (user?.activeProfile !== 'RESCATISTA') {
      navigate('/')
      return
    }
    if (!user?.tieneTienda) {
      navigate('/abrir-tienda')
      return
    }
    cargarItems()
  }, [user])

  async function cargarItems() {
    try {
      const res = await getMisItems()
      setItems(res.data)
    } catch {
      setError('Error al cargar los ítems.')
    } finally {
      setCargando(false)
    }
  }

  // --- crear ---

  function handleFotosCrear(e) {
    const archivos = Array.from(e.target.files)
    if (archivos.length < 1 || archivos.length > 5) {
      setErrorCrear('Tenés que subir entre 1 y 5 fotos.')
      setFotosCrear([])
      return
    }
    setErrorCrear('')
    setFotosCrear(archivos)
  }

  async function handleCrear(e) {
    e.preventDefault()
    setErrorCrear('')
    if (!formCrear.tipo) { setErrorCrear('El tipo es obligatorio.'); return }
    if (formCrear.stock === '') { setErrorCrear('El stock es obligatorio.'); return }
    if (fotosCrear.length < 1) { setErrorCrear('Tenés que subir al menos una foto.'); return }
    setCreando(true)
    try {
      const formData = new FormData()
      formData.append('titulo', formCrear.titulo)
      formData.append('tipo', formCrear.tipo)
      if (formCrear.descripcion) formData.append('descripcion', formCrear.descripcion)
      if (formCrear.precio) formData.append('precio', formCrear.precio)
      formData.append('stock', formCrear.stock)
      fotosCrear.forEach(f => formData.append('fotos', f))
      const res = await crearItem(formData)
      setItems(prev => [res.data, ...prev])
      setFormCrear(formInicial)
      setFotosCrear([])
      setMostrarFormCrear(false)
    } catch (err) {
      setErrorCrear(err.response?.data || 'Error al crear el ítem.')
    } finally {
      setCreando(false)
    }
  }

  // --- editar ---

  function abrirEditar(item) {
    setEditandoId(item.id)
    setFormEditar({
      titulo: item.titulo,
      tipo: item.tipo,
      descripcion: item.descripcion || '',
      precio: item.precio ?? '',
      stock: item.stock ?? '',
    })
    setErrorEditar('')
  }

  async function handleEditar(e) {
    e.preventDefault()
    setErrorEditar('')
    if (!formEditar.tipo) { setErrorEditar('El tipo es obligatorio.'); return }
    if (formEditar.stock === '') { setErrorEditar('El stock es obligatorio.'); return }
    setEditando(true)
    try {
      const body = {
        titulo: formEditar.titulo,
        tipo: formEditar.tipo,
        descripcion: formEditar.descripcion || null,
        precio: formEditar.precio !== '' ? Number(formEditar.precio) : null,
        stock: Number(formEditar.stock),
      }
      const res = await editarItem(editandoId, body)
      setItems(prev => prev.map(it => it.id === editandoId ? res.data : it))
      setEditandoId(null)
    } catch (err) {
      setErrorEditar(err.response?.data || 'Error al guardar los cambios.')
    } finally {
      setEditando(false)
    }
  }

  // --- agregar fotos ---

  function handleFotosNuevas(e) {
    const archivos = Array.from(e.target.files)
    setFotosNuevas(archivos)
    setErrorFotos('')
  }

  async function handleAgregarFotos(e) {
    e.preventDefault()
    if (fotosNuevas.length < 1) { setErrorFotos('Seleccioná al menos una foto.'); return }
    setAgregandoFotos(true)
    try {
      const formData = new FormData()
      fotosNuevas.forEach(f => formData.append('fotos', f))
      const res = await agregarFotosItem(agregandoFotosId, formData)
      setItems(prev => prev.map(it => it.id === agregandoFotosId ? res.data : it))
      setAgregandoFotosId(null)
      setFotosNuevas([])
    } catch (err) {
      setErrorFotos(err.response?.data || 'Error al subir las fotos.')
    } finally {
      setAgregandoFotos(false)
    }
  }

  // --- eliminar foto ---

  async function handleEliminarFoto(itemId, fotoId) {
    if (!confirm('¿Querés eliminar esta foto?')) return
    try {
      const res = await eliminarFotoItem(itemId, fotoId)
      setItems(prev => prev.map(it => it.id === itemId ? res.data : it))
    } catch (err) {
      alert(err.response?.data || 'Error al eliminar la foto.')
    }
  }

  // --- eliminar item ---

  async function handleEliminar(id) {
    if (!confirm('¿Estás seguro de que querés eliminar este ítem? Esta acción no se puede deshacer.')) return
    try {
      await eliminarItem(id)
      setItems(prev => prev.filter(it => it.id !== id))
    } catch (err) {
      alert(err.response?.data || 'Error al eliminar el ítem.')
    }
  }

  if (cargando) return <p>Cargando...</p>

  return (
    <div style={{ maxWidth: 800, margin: '0 auto', padding: '0 16px' }}>
      <h2>Mi tienda</h2>

      {error && <p style={{ color: 'red' }}>{error}</p>}

      <button onClick={() => { setMostrarFormCrear(v => !v); setErrorCrear('') }}>
        {mostrarFormCrear ? 'Cancelar' : '+ Agregar ítem'}
      </button>

      {mostrarFormCrear && (
        <form onSubmit={handleCrear} style={{ border: '1px solid #ccc', padding: 16, margin: '16px 0', display: 'flex', flexDirection: 'column', gap: 8 }}>
          <h3>Nuevo ítem</h3>

          <label>Título *
            <input
              type="text"
              value={formCrear.titulo}
              onChange={e => setFormCrear(f => ({ ...f, titulo: e.target.value }))}
              required
              maxLength={100}
            />
          </label>

          <label>Tipo *
            <select
              value={formCrear.tipo}
              onChange={e => setFormCrear(f => ({ ...f, tipo: e.target.value }))}
              required
            >
              <option value="">Seleccioná un tipo</option>
              {TIPOS_ITEM.map(t => (
                <option key={t.value} value={t.value}>{t.label}</option>
              ))}
            </select>
          </label>

          <label>Descripción
            <textarea
              value={formCrear.descripcion}
              onChange={e => setFormCrear(f => ({ ...f, descripcion: e.target.value }))}
              rows={3}
            />
          </label>

          <label>Precio (ARS)
            <input
              type="number"
              min="0.01"
              step="0.01"
              value={formCrear.precio}
              onChange={e => setFormCrear(f => ({ ...f, precio: e.target.value }))}
            />
          </label>

          <label>Stock *
            <input
              type="number"
              min="0"
              step="1"
              value={formCrear.stock}
              onChange={e => setFormCrear(f => ({ ...f, stock: e.target.value }))}
              required
            />
          </label>

          <label>Fotos (1 a 5) *
            <input type="file" accept="image/*" multiple onChange={handleFotosCrear} />
          </label>
          {fotosCrear.length > 0 && <span>{fotosCrear.length} foto(s) seleccionada(s)</span>}

          {errorCrear && <p style={{ color: 'red', margin: 0 }}>{errorCrear}</p>}

          <button type="submit" disabled={creando}>
            {creando ? 'Creando...' : 'Crear ítem'}
          </button>
        </form>
      )}

      {items.length === 0 && !mostrarFormCrear && (
        <p>Todavía no tenés ítems en tu tienda.</p>
      )}

      {items.map(item => (
        <div key={item.id} style={{ border: '1px solid #ddd', padding: 16, margin: '16px 0', borderRadius: 6 }}>

          {editandoId === item.id ? (
            <form onSubmit={handleEditar} style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
              <label>Título *
                <input
                  type="text"
                  value={formEditar.titulo}
                  onChange={e => setFormEditar(f => ({ ...f, titulo: e.target.value }))}
                  required
                  maxLength={100}
                />
              </label>

              <label>Tipo *
                <select
                  value={formEditar.tipo}
                  onChange={e => setFormEditar(f => ({ ...f, tipo: e.target.value }))}
                  required
                >
                  {TIPOS_ITEM.map(t => (
                    <option key={t.value} value={t.value}>{t.label}</option>
                  ))}
                </select>
              </label>

              <label>Descripción
                <textarea
                  value={formEditar.descripcion}
                  onChange={e => setFormEditar(f => ({ ...f, descripcion: e.target.value }))}
                  rows={3}
                />
              </label>

              <label>Precio (ARS)
                <input
                  type="number"
                  min="0.01"
                  step="0.01"
                  value={formEditar.precio}
                  onChange={e => setFormEditar(f => ({ ...f, precio: e.target.value }))}
                />
              </label>

              <label>Stock *
                <input
                  type="number"
                  min="0"
                  step="1"
                  value={formEditar.stock}
                  onChange={e => setFormEditar(f => ({ ...f, stock: e.target.value }))}
                  required
                />
              </label>

              {errorEditar && <p style={{ color: 'red', margin: 0 }}>{errorEditar}</p>}

              <div style={{ display: 'flex', gap: 8 }}>
                <button type="submit" disabled={editando}>{editando ? 'Guardando...' : 'Guardar'}</button>
                <button type="button" onClick={() => setEditandoId(null)}>Cancelar</button>
              </div>
            </form>
          ) : (
            <>
              <div>
                <strong>{item.titulo}</strong>
                {' · '}
                <span>{TIPOS_ITEM.find(t => t.value === item.tipo)?.label ?? item.tipo}</span>
                {item.precio != null && <span> · ${formatMonto(item.precio)}</span>}
                <span> · Stock: {item.stock}</span>
              </div>

              {item.descripcion && <p style={{ margin: '8px 0' }}>{item.descripcion}</p>}

              {/* fotos */}
              {item.fotos.filter(f => f.estado !== 'ELIMINADA').length > 0 && (
                <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', margin: '8px 0' }}>
                  {item.fotos
                    .filter(f => f.estado !== 'ELIMINADA')
                    .map(foto => (
                      <div key={foto.id} style={{ position: 'relative' }}>
                        <img
                          src={`${import.meta.env.VITE_API_URL?.replace('/api', '') ?? 'http://localhost:8080'}${foto.url}`}
                          alt="foto ítem"
                          style={{ width: 100, height: 100, objectFit: 'cover', borderRadius: 4 }}
                        />
                        <button
                          style={{ position: 'absolute', top: 2, right: 2, fontSize: 11, padding: '1px 4px' }}
                          onClick={() => handleEliminarFoto(item.id, foto.id)}
                        >
                          Eliminar
                        </button>
                      </div>
                    ))}
                </div>
              )}

              <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginTop: 8 }}>
                <button onClick={() => abrirEditar(item)}>Editar</button>

                <button onClick={() => { setAgregandoFotosId(item.id); setFotosNuevas([]); setErrorFotos('') }}>
                  + Agregar fotos
                </button>

                <button
                  style={{ color: 'red' }}
                  onClick={() => handleEliminar(item.id)}
                >
                  Eliminar
                </button>
              </div>

              {agregandoFotosId === item.id && (
                <form onSubmit={handleAgregarFotos} style={{ marginTop: 8, display: 'flex', flexDirection: 'column', gap: 6 }}>
                  <input type="file" accept="image/*" multiple onChange={handleFotosNuevas} />
                  {errorFotos && <p style={{ color: 'red', margin: 0 }}>{errorFotos}</p>}
                  <div style={{ display: 'flex', gap: 8 }}>
                    <button type="submit" disabled={agregandoFotos}>
                      {agregandoFotos ? 'Subiendo...' : 'Subir fotos'}
                    </button>
                    <button type="button" onClick={() => setAgregandoFotosId(null)}>Cancelar</button>
                  </div>
                </form>
              )}
            </>
          )}
        </div>
      ))}
    </div>
  )
}

export default MiTienda
