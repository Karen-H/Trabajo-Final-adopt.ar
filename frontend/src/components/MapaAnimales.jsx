import { useEffect, useMemo } from 'react'
import { MapContainer, TileLayer, Marker, Popup, useMap } from 'react-leaflet'
import MarkerClusterGroup from 'react-leaflet-cluster'
import { Link } from 'react-router-dom'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import 'leaflet.markercluster/dist/MarkerCluster.css'
import 'leaflet.markercluster/dist/MarkerCluster.Default.css'
import iconUrl from 'leaflet/dist/images/marker-icon.png'
import iconRetinaUrl from 'leaflet/dist/images/marker-icon-2x.png'
import shadowUrl from 'leaflet/dist/images/marker-shadow.png'

// el ícono por defecto de leaflet no resuelve sus rutas con el bundler, lo armamos a mano
const icono = L.icon({
  iconUrl,
  iconRetinaUrl,
  shadowUrl,
  iconSize: [25, 41],
  iconAnchor: [12, 41],
  popupAnchor: [1, -34],
  shadowSize: [41, 41],
})

const ETIQUETA_TIPO = { PERRO: 'Perro', GATO: 'Gato', OTRO: 'Otro' }

const CENTRO_AR = [-38.4, -63.6]

function AjustarEncuadre({ puntos }) {
  const map = useMap()
  useEffect(() => {
    if (puntos.length === 0) return
    const bounds = L.latLngBounds(puntos.map(p => [p.lat, p.lng]))
    map.fitBounds(bounds, { padding: [40, 40], maxZoom: 14 })
  }, [puntos, map])
  return null
}

function MapaAnimales({ animales }) {
  const puntos = useMemo(
    () => animales
      .filter(a => a.latitud != null && a.longitud != null)
      .map(a => ({ animal: a, lat: a.latitud, lng: a.longitud })),
    [animales]
  )

  return (
    <MapContainer
      center={CENTRO_AR}
      zoom={4}
      style={{ height: '100%', width: '100%', borderRadius: 6 }}
    >
      <TileLayer
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
      />
      <AjustarEncuadre puntos={puntos} />
      <MarkerClusterGroup chunkedLoading spiderfyOnMaxZoom maxClusterRadius={50}>
        {puntos.map(({ animal: a, lat, lng }) => (
          <Marker key={a.id} position={[lat, lng]} icon={icono}>
            <Popup>
              <Link
                to={`/animal/${a.id}`}
                style={{ color: 'inherit', textDecoration: 'none', display: 'block', minWidth: 140 }}
              >
                {a.fotos?.length > 0 && (
                  <img
                    src={a.fotos[0].url}
                    alt={a.nombre || a.tipo}
                    style={{ width: '100%', height: 90, objectFit: 'cover', borderRadius: 4, marginBottom: 6 }}
                  />
                )}
                <p style={{ margin: '0 0 2px', fontWeight: 600 }}>{a.nombre || ETIQUETA_TIPO[a.tipo]}</p>
                {(a.ciudad || a.provincia) && (
                  <p style={{ margin: '0 0 4px', fontSize: 12, color: '#666' }}>
                    {[a.ciudad, a.provincia].filter(Boolean).join(', ')}
                  </p>
                )}
                {a.descripcion && (
                  <p style={{ margin: '0 0 4px', fontSize: 12 }}>
                    {a.descripcion.length > 80 ? a.descripcion.slice(0, 80) + '…' : a.descripcion}
                  </p>
                )}
                <span style={{ fontSize: 12, color: '#1a6e2e', fontWeight: 600 }}>Ver detalle →</span>
              </Link>
            </Popup>
          </Marker>
        ))}
      </MarkerClusterGroup>
    </MapContainer>
  )
}

export default MapaAnimales
