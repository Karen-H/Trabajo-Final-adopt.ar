import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { getAnimalById, registrarVista } from '../api/animal'
import { getFavoritos, agregarFavorito, quitarFavorito } from '../api/favorito'
import { getMisBloqueos } from '../api/reserva'
import { iniciarChat } from '../api/chat'
import ModalDenuncia from '../components/ModalDenuncia'
import ModeracionPublicacion from '../components/ModeracionPublicacion'
import { useAuth } from '../context/AuthContext'

const ETIQUETA_TIPO = { PERRO: 'Perro', GATO: 'Gato', OTRO: 'Otro' }
const ETIQUETA_SEXO = { MACHO: 'Macho', HEMBRA: 'Hembra' }
const ETIQUETA_EDAD = {
  CACHORRO: 'Cachorro (0-6 meses)',
  JOVEN: 'Joven (6 meses - 2 años)',
  ADULTO: 'Adulto (2-7 años)',
  SENIOR: 'Senior (7+ años)',
}

function AnimalDetalle() {
  const { id } = useParams()
  const navigate = useNavigate()
  const { user } = useAuth()
  const estaLogueado = !!localStorage.getItem('token')

  const [animal, setAnimal] = useState(null)
  const [cargando, setCargando] = useState(true)
  const [noEncontrado, setNoEncontrado] = useState(false)
  const [esFavorito, setEsFavorito] = useState(false)
  const [bloqueado, setBloqueado] = useState(false)
  const [denunciaOpen, setDenunciaOpen] = useState(false)
  const [denunciado, setDenunciado] = useState(false)

  useEffect(() => {
    getAnimalById(id)
      .then(r => setAnimal(r.data))
      .catch(err => {
        if (err.response?.status === 404) setNoEncontrado(true)
        else navigate(-1)
      })
      .finally(() => setCargando(false))

    // el backend no cuenta la vista si el caller es el dueño
    registrarVista(id).catch(() => {})

    if (estaLogueado) {
      getFavoritos()
        .then(r => setEsFavorito(r.data.some(f => f.animalId === Number(id))))
        .catch(() => {})
      getMisBloqueos()
        .then(r => setBloqueado(r.data.includes(Number(id))))
        .catch(() => {})
    }
  }, [id])

  async function toggleFavorito() {
    if (!estaLogueado) {
      localStorage.setItem('pendingFavorito', id)
      navigate('/login')
      return
    }
    if (esFavorito) {
      await quitarFavorito(id)
      setEsFavorito(false)
    } else {
      await agregarFavorito(id)
      setEsFavorito(true)
    }
  }

  if (cargando) return <p>Cargando...</p>
  if (noEncontrado) return <p>Animal no encontrado.</p>
  if (!animal) return null

  const esAdopcion = animal.categoria === 'ADOPCION'
  const esMio = estaLogueado && animal.usuarioId === user?.id
  const esAdoptante = user?.activeProfile === 'ADOPTANTE'
  const esModerador = user?.role === 'ADMIN' || user?.role === 'MODERADOR'
  const amigables = [
    animal.amigableConGatos && 'gatos',
    animal.amigableConPerros && 'perros',
    animal.amigableConNinos && 'niños',
  ].filter(Boolean)

  return (
    <div>
      <button onClick={() => navigate(-1)} style={{ marginBottom: 16 }}>Volver</button>

      <h2>{animal.nombre || ETIQUETA_TIPO[animal.tipo]}</h2>

      {animal.fotos?.length > 0 && (
        <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', margin: '12px 0' }}>
          {animal.fotos.map(f => (
            <img key={f.id} src={f.url} alt={animal.nombre || animal.tipo} style={{ width: 200, height: 200, objectFit: 'cover', borderRadius: 4 }} />
          ))}
        </div>
      )}

      <p><strong>Tipo:</strong> {ETIQUETA_TIPO[animal.tipo]}</p>
      {animal.sexo && <p><strong>Sexo:</strong> {ETIQUETA_SEXO[animal.sexo]}</p>}
      {animal.edad && <p><strong>Edad:</strong> {ETIQUETA_EDAD[animal.edad]}</p>}

      {esAdopcion && (
        <>
          {animal.tipoAdopcion && (
            <p><strong>Tipo de adopción:</strong> {animal.tipoAdopcion === 'PERMANENTE' ? 'Permanente' : 'Tránsito'}</p>
          )}
          {amigables.length > 0 && (
            <p><strong>Amigable con:</strong> {amigables.join(', ')}</p>
          )}
        </>
      )}

      {!esAdopcion && (
        <>
          {animal.direccion && <p><strong>Visto en:</strong> {animal.direccion}</p>}
          {animal.fechaAvistamiento && <p><strong>Fecha:</strong> {animal.fechaAvistamiento}</p>}
          {animal.enPosesionDelPublicador != null && (
            <p><strong>En posesión del publicador:</strong> {animal.enPosesionDelPublicador ? 'Sí' : 'No'}</p>
          )}
        </>
      )}

      {animal.descripcion && <p><strong>Descripción:</strong> {animal.descripcion}</p>}

      <p style={{ fontSize: 13, color: '#666' }}>
        Publicado por {animal.rescatistaNombre}
        {(animal.ciudad || animal.provincia) && ` · ${[animal.ciudad, animal.provincia].filter(Boolean).join(', ')}`}
        {animal.organizacion && ` · ${animal.organizacion}`}
      </p>

      {/* acciones */}
      {!esModerador && (
        <div style={{ display: 'flex', gap: 12, alignItems: 'center', marginTop: 16, flexWrap: 'wrap' }}>
          {!esMio && (
            <button onClick={toggleFavorito}>
              {esFavorito ? 'Eliminar de favoritos' : 'Guardar como favorito'}
            </button>
          )}

          {estaLogueado && !esMio && esAdopcion && esAdoptante && (
            bloqueado ? (
              <span style={{ fontSize: 12, color: '#999' }}>No disponible para vos (reserva cancelada)</span>
            ) : (
              <button
                onClick={async () => {
                  try {
                    await iniciarChat(animal.usuarioId, animal.id, animal.nombre + ' (adopción)')
                    navigate('/chats')
                  } catch (e) {
                    alert(e.response?.data || 'No se pudo iniciar el chat.')
                  }
                }}
                style={{ fontSize: 13 }}
              >
                Consultar al rescatista
              </button>
            )
          )}

          {estaLogueado && !esMio && !esAdopcion && (
            <button
              onClick={async () => {
                try {
                  const label = (animal.nombre || ETIQUETA_TIPO[animal.tipo]) + (animal.estado === 'PERDIDO' ? ' (perdido)' : ' (encontrado)')
                  await iniciarChat(animal.usuarioId, animal.id, label)
                  navigate('/chats')
                } catch (e) {
                  alert(e.response?.data || 'No se pudo iniciar el chat.')
                }
              }}
              style={{ fontSize: 13 }}
            >
              Contactar al publicador
            </button>
          )}

          {estaLogueado && !esMio && (
            <button
              onClick={() => {
                if (denunciado) { alert('Ya denunciaste esta publicación'); return }
                setDenunciaOpen(true)
              }}
              style={{ background: 'none', border: 'none', cursor: 'pointer', fontSize: 12, color: '#999', padding: 0 }}
              title="Reportar publicación"
            >
              Reportar
            </button>
          )}
        </div>
      )}

      {esModerador && (
        <ModeracionPublicacion
          animal={animal}
          onEliminada={() => navigate(-1)}
          onFotoEliminada={(_animalId, fotoId) => setAnimal(prev => ({
            ...prev,
            fotos: prev.fotos.map(f => f.id === fotoId ? { ...f, estado: 'ELIMINADA' } : f),
          }))}
        />
      )}

      {denunciaOpen && (
        <ModalDenuncia
          animalId={animal.id}
          onClose={() => setDenunciaOpen(false)}
          onSuccess={() => {
            setDenunciado(true)
            setDenunciaOpen(false)
            alert('Denuncia enviada. El administrador la revisará.')
          }}
        />
      )}
    </div>
  )
}

export default AnimalDetalle
