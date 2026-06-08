package com.adoptar.service;

import com.adoptar.dto.request.IniciarChatRequest;
import com.adoptar.dto.response.ChatResumenResponse;
import com.adoptar.dto.response.MensajeResponse;
import com.adoptar.entity.Animal;
import com.adoptar.entity.Chat;
import com.adoptar.entity.Mensaje;
import com.adoptar.entity.User;
import com.adoptar.repository.AnimalRepository;
import com.adoptar.repository.BloqueoAdopcionRepository;
import com.adoptar.repository.ChatRepository;
import com.adoptar.repository.MensajeRepository;
import com.adoptar.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepository;
    private final MensajeRepository mensajeRepository;
    private final UserRepository userRepository;
    private final AnimalRepository animalRepository;
    private final BloqueoAdopcionRepository bloqueoRepository;

    // inicia o recupera un chat; agrega mensaje del sistema indicando el animal
    @Transactional
    public Map<String, Object> iniciarChat(User adoptante, IniciarChatRequest req) {
        User rescatista = userRepository.findById(req.getRescatistaId())
                .orElseThrow(() -> new RuntimeException("Rescatista no encontrado"));

        Animal animal = animalRepository.findById(req.getAnimalId())
                .orElseThrow(() -> new RuntimeException("Animal no encontrado"));

        // verificar bloqueo para este adoptante + animal
        bloqueoRepository.findByAdoptanteAndAnimal(adoptante, animal).ifPresent(b -> {
            if (b.getBloqueadoHasta().isAfter(LocalDateTime.now())) {
                throw new RuntimeException("Tenés una restricción para adoptar este animal hasta "
                        + b.getBloqueadoHasta().toLocalDate());
            }
        });

        boolean esNuevo = false;
        Chat chat = chatRepository.findByAdoptanteAndRescatista(adoptante, rescatista)
                .orElse(null);

        if (chat == null) {
            chat = chatRepository.save(Chat.builder()
                    .adoptante(adoptante)
                    .rescatista(rescatista)
                    .build());
            esNuevo = true;
        }

        // agregar el animal a la lista del chat si no está ya
        final Chat chatFinal = chat;
        boolean yaEsta = chat.getAnimales().stream().anyMatch(a -> a.getId().equals(animal.getId()));
        if (!yaEsta) {
            chatFinal.getAnimales().add(animal);
            chatRepository.save(chatFinal);
        }

        // mensaje del sistema con el animal que motivó el chat
        String textoSistema = esNuevo
                ? adoptante.getNombre() + " " + adoptante.getApellido() + " inició un chat por " + req.getAnimalNombre()
                : adoptante.getNombre() + " " + adoptante.getApellido() + " también preguntó por " + req.getAnimalNombre();

        mensajeRepository.save(Mensaje.builder()
                .chat(chatFinal)
                .emisor(null)
                .contenido(textoSistema)
                .leido(false)
                .build());

        return Map.of("chatId", chatFinal.getId());
    }

    // lista de chats del usuario con resumen
    @Transactional(readOnly = true)
    public List<ChatResumenResponse> getMisChats(User user) {
        return chatRepository.findByParticipante(user).stream().map(chat -> {
            List<Mensaje> mensajes = mensajeRepository.findByChatOrderByCreadoEnAsc(chat);
            Mensaje ultimo = mensajes.isEmpty() ? null : mensajes.get(mensajes.size() - 1);

            // "el otro" desde la perspectiva del usuario
            boolean esAdoptante = chat.getAdoptante().getId().equals(user.getId());
            User otro = esAdoptante ? chat.getRescatista() : chat.getAdoptante();

            long noLeidos = mensajes.stream()
                    .filter(m -> !m.isLeido() && (m.getEmisor() == null || !m.getEmisor().getId().equals(user.getId())))
                    .count();

            return ChatResumenResponse.builder()
                    .id(chat.getId())
                    .otroUsuarioId(otro.getId())
                    .otroUsuarioNombre(otro.getNombre() + " " + otro.getApellido())
                    .ultimoMensaje(ultimo != null ? ultimo.getContenido() : "")
                    .ultimoMensajeEn(ultimo != null ? ultimo.getCreadoEn() : chat.getCreadoEn())
                    .noLeidos(noLeidos)
                    .build();
        }).toList();
    }

    // mensajes de un chat; marca los no leídos como leídos
    @Transactional
    public List<MensajeResponse> getMensajes(Long chatId, User user) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Chat no encontrado"));

        // solo los participantes pueden ver el chat
        boolean participa = chat.getAdoptante().getId().equals(user.getId())
                || chat.getRescatista().getId().equals(user.getId());
        if (!participa) throw new RuntimeException("No autorizado");

        mensajeRepository.marcarLeidosEnChat(chat, user);

        return mensajeRepository.findByChatOrderByCreadoEnAsc(chat).stream()
                .map(m -> MensajeResponse.builder()
                        .id(m.getId())
                        .emisorId(m.getEmisor() != null ? m.getEmisor().getId() : null)
                        .emisorNombre(m.getEmisor() != null
                                ? m.getEmisor().getNombre() + " " + m.getEmisor().getApellido()
                                : "Sistema")
                        .contenido(m.getContenido())
                        .creadoEn(m.getCreadoEn())
                        .esPropio(m.getEmisor() != null && m.getEmisor().getId().equals(user.getId()))
                        .build())
                .toList();
    }

    // enviar un mensaje
    @Transactional
    public MensajeResponse enviarMensaje(Long chatId, User emisor, String contenido) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Chat no encontrado"));

        boolean participa = chat.getAdoptante().getId().equals(emisor.getId())
                || chat.getRescatista().getId().equals(emisor.getId());
        if (!participa) throw new RuntimeException("No autorizado");

        Mensaje m = mensajeRepository.save(Mensaje.builder()
                .chat(chat)
                .emisor(emisor)
                .contenido(contenido)
                .leido(false)
                .build());

        return MensajeResponse.builder()
                .id(m.getId())
                .emisorId(emisor.getId())
                .emisorNombre(emisor.getNombre() + " " + emisor.getApellido())
                .contenido(m.getContenido())
                .creadoEn(m.getCreadoEn())
                .esPropio(true)
                .build();
    }

    // total de mensajes no leídos para el badge
    @Transactional(readOnly = true)
    public long getNoLeidos(User user) {
        return mensajeRepository.countNoLeidosByUser(user);
    }
}
