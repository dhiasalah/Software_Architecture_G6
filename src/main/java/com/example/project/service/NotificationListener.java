package com.example.project.service;

import com.example.project.dto.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * SERVICE NOTIFICATION LISTENER - Le consommateur RabbitMQ
 *
 * C'est LE CŒUR DU DÉCOUPLAGE.
 *
 * COMMENT ÇA MARCHE ?
 * 1. AuthController.register() publie un événement "UserRegistered" dans RabbitMQ
 * 2. RabbitMQ stocke le message dans la queue "notification.user-registered"
 * 3. Cette classe ÉCOUTE cette queue grâce à @RabbitListener
 * 4. Dès qu'un message arrive, la méthode handleUserRegistered() est appelée AUTOMATIQUEMENT
 * 5. Elle construit le lien de vérification et envoie un e-mail
 *
 * POURQUOI C'EST DÉCOUPLÉ ?
 * - AuthController ne sait même pas que ce listener existe
 * - Si ce listener est en panne, les messages s'accumulent dans la queue
 *   et seront traités dès qu'il redémarre (rien n'est perdu)
 * - On pourrait avoir PLUSIEURS listeners (ex: un qui envoie des SMS, un qui fait de l'analytics)
 *   sans toucher au code d'Auth
 *
 * OÙ VOIR LES E-MAILS ?
 * Les e-mails sont envoyés via MailHog (serveur SMTP de développement).
 * Ouvrez http://localhost:8025 dans votre navigateur pour voir les e-mails reçus.
 * MailHog ne les envoie PAS vraiment — il les capture pour qu'on puisse les lire.
 */
@Service
@RequiredArgsConstructor
public class NotificationListener {

    /**
     * JavaMailSender = l'outil de Spring pour envoyer des e-mails
     * Configuré dans application.properties (host=localhost, port=1025 = MailHog)
     */
    private final JavaMailSender mailSender;

    /**
     * MÉTHODE PRINCIPALE : Consomme l'événement UserRegistered
     *
     * @RabbitListener(queues = "notification.user-registered")
     * → Dit à Spring : "Écoute la queue 'notification.user-registered'.
     *   Quand un message arrive, convertis-le en UserRegisteredEvent et appelle cette méthode."
     *
     * Si cette méthode PLANTE (exception), le message sera :
     * 1. Réessayé automatiquement par RabbitMQ
     * 2. Si ça échoue encore → envoyé dans la DLQ (Dead Letter Queue)
     *    → On peut le voir dans le panneau RabbitMQ http://localhost:15672
     *
     * @param event L'événement reçu (converti automatiquement depuis le JSON dans RabbitMQ)
     */
    @RabbitListener(queues = "${app.mq.queue.user-registered}")
    public void handleUserRegistered(UserRegisteredEvent event) {
        System.out.println("📬 Événement UserRegistered reçu !");
        System.out.println("   eventId  = " + event.getEventId());
        System.out.println("   userId   = " + event.getUserId());
        System.out.println("   email    = " + event.getEmail());
        System.out.println("   tokenId  = " + event.getTokenId());

        // --- Construire le lien de vérification ---
        // C'est le lien que l'utilisateur va cliquer dans son e-mail
        // Il contient :
        //   - tokenId : pour retrouver le token en base
        //   - t : le token en clair, pour le comparer au hash stocké
        String verificationLink = String.format(
                "http://localhost:8080/api/auth/verify?tokenId=%s&t=%s",
                event.getTokenId(),
                event.getTokenClear()
        );

        // --- Construire et envoyer l'e-mail ---
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(event.getEmail());                              // Destinataire
        message.setFrom("noreply@auth-service.local");                // Expéditeur (fictif)
        message.setSubject("Vérifiez votre adresse e-mail");          // Objet
        message.setText(
                "Bonjour,\n\n" +
                "Merci pour votre inscription !\n\n" +
                "Pour activer votre compte, cliquez sur le lien ci-dessous :\n\n" +
                verificationLink + "\n\n" +
                "Ce lien est valide pendant 30 minutes.\n" +
                "Si vous n'avez pas demandé cette inscription, ignorez cet e-mail.\n\n" +
                "Cordialement,\n" +
                "L'équipe Auth Service"
        );

        mailSender.send(message);

        System.out.println("✅ E-mail de vérification envoyé à " + event.getEmail());
        System.out.println("   Lien : " + verificationLink);
        System.out.println("   📧 Consultez http://localhost:8025 pour voir l'e-mail (MailHog)");
    }
}
