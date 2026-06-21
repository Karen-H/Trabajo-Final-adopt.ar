import { useState } from 'react'
import { eliminarAnimalAdmin, eliminarFotoAdmin } from '../api/admin'

// controles de moderacion (admin/moderador) sobre una publicacion ya visible al publico
function ModeracionPublicacion({ animal, onEliminada, onFotoEliminada }) {
  const [motivoEliminar, setMotivoEliminar] = useState('')
  const [motivoFoto, setMotivoFoto] = useState({})

  async function handleEliminarPublicacion() {
    if (!motivoEliminar.trim()) { alert('Ingresá un motivo.'); return }
    if (!confirm('¿Eliminar esta publicación? Esta acción no puede deshacerse por el usuario.')) return
    try {
      await eliminarAnimalAdmin(animal.id, motivoEliminar)
      onEliminada(animal.id)
    } catch (err) {
      alert(err.response?.data || 'No se pudo eliminar la publicación.')
    }
  }

  async function handleEliminarFoto(fotoId) {
    const motivo = motivoFoto[fotoId] || ''
    if (!motivo.trim()) { alert('Ingresá un motivo.'); return }
    if (!confirm('¿Eliminar esta foto?')) return
    try {
      await eliminarFotoAdmin(fotoId, motivo)
      onFotoEliminada(animal.id, fotoId)
    } catch (err) {
      alert(err.response?.data || 'No se pudo eliminar la foto.')
    }
  }

  const fotosAprobadas = (animal.fotos || []).filter(f => !f.estado || f.estado === 'APROBADA')

  return (
    <div style={{ border: '1px dashed #c00', borderRadius: 4, padding: 8, margin: '0 12px 12px', background: '#fff5f5' }}>
      <p style={{ margin: '0 0 6px', fontSize: 12, fontWeight: 600, color: '#c00' }}>Moderación</p>

      {fotosAprobadas.length > 0 && (
        <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginBottom: 8 }}>
          {fotosAprobadas.map(foto => (
            <div key={foto.id} style={{ textAlign: 'center' }}>
              <input
                type="text"
                placeholder="Motivo"
                value={motivoFoto[foto.id] || ''}
                onChange={e => setMotivoFoto(prev => ({ ...prev, [foto.id]: e.target.value }))}
                style={{ width: 90, fontSize: 11 }}
              />
              <button onClick={() => handleEliminarFoto(foto.id)} style={{ display: 'block', margin: '2px auto 0', fontSize: 11, color: '#c00' }}>
                Eliminar foto
              </button>
            </div>
          ))}
        </div>
      )}

      <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap', alignItems: 'center' }}>
        <input
          type="text"
          placeholder="Motivo de eliminación"
          value={motivoEliminar}
          onChange={e => setMotivoEliminar(e.target.value)}
          style={{ flex: 1, minWidth: 160, fontSize: 12 }}
        />
        <button onClick={handleEliminarPublicacion} style={{ color: '#c00', fontSize: 12 }}>
          Eliminar publicación
        </button>
      </div>
    </div>
  )
}

export default ModeracionPublicacion
