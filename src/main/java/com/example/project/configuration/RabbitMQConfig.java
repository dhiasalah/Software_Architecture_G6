package com.example.project.configuration;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * CONFIGURATION RABBITMQ - Expliquée simplement
 *
 * Cette classe configure toute la "plomberie" de la messagerie RabbitMQ.
 *
 * VOCABULAIRE RABBITMQ (imaginez un bureau de poste) :
 *
 * 1. EXCHANGE (= le bureau de tri postal)
 *    → Reçoit les messages et les ROUTE vers les bonnes files d'attente
 *    → Type "topic" = le routage se fait par "routing key" (ex: "auth.user-registered")
 *
 * 2. QUEUE (= la boîte aux lettres)
 *    → Stocke les messages en attendant qu'un consommateur les lise
 *    → Les messages restent dans la queue même si personne ne les lit (résilience)
 *
 * 3. BINDING (= la règle de tri)
 *    → Relie un exchange à une queue avec un pattern de routing key
 *    → Ex: "auth.user-registered" → messages d'inscription vers la queue notification
 *
 * 4. DLQ = Dead Letter Queue (= file des lettres non distribuables)
 *    → Si un message échoue (erreur, rejet), il va dans la DLQ au lieu d'être perdu
 *    → Permet de débugger et de retraiter les messages en erreur
 *
 * FLUX COMPLET :
 * AuthController.register()
 *   → publie sur exchange "auth.events" avec routing key "auth.user-registered"
 *     → RabbitMQ route vers queue "notification.user-registered"
 *       → NotificationListener consomme le message et envoie l'e-mail
 *         → Si erreur → message va dans DLQ "notification.user-registered.dlq"
 */
@Configuration
public class RabbitMQConfig {

    // On lit les noms depuis application.properties (pas de valeurs en dur dans le code)
    @Value("${app.mq.exchange}")
    private String exchangeName;

    @Value("${app.mq.rk.user-registered}")
    private String userRegisteredRoutingKey;

    @Value("${app.mq.queue.user-registered}")
    private String userRegisteredQueue;

    @Value("${app.mq.queue.user-registered-dlq}")
    private String userRegisteredDlq;

    // =====================================================================
    // 1. EXCHANGE - Le bureau de tri
    // =====================================================================

    /**
     * Crée un exchange de type "topic"
     *
     * Topic = le routage se fait par PATTERN sur la routing key
     * Exemple : "auth.user-registered" matche le pattern "auth.user-registered"
     *
     * durable(true) = l'exchange survit au redémarrage de RabbitMQ
     */
    @Bean
    public TopicExchange authExchange() {
        return ExchangeBuilder
                .topicExchange(exchangeName)
                .durable(true)
                .build();
    }

    // =====================================================================
    // 2. DLQ - La file des messages en erreur
    // =====================================================================

    /**
     * Dead Letter Queue (DLQ)
     *
     * C'est une queue "poubelle de récupération" :
     * - Si le consumer NotificationListener plante (ex: SMTP down)
     * - Le message n'est PAS perdu → il est envoyé dans cette DLQ
     * - On peut ensuite inspecter les messages en erreur dans le panneau RabbitMQ
     *   (http://localhost:15672)
     */
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder
                .durable(userRegisteredDlq)
                .build();
    }

    // =====================================================================
    // 3. QUEUE PRINCIPALE - La boîte aux lettres du service Notification
    // =====================================================================

    /**
     * Queue principale pour les événements "UserRegistered"
     *
     * Configuration importante :
     * - x-dead-letter-exchange = "" → en cas d'erreur, envoyer à l'exchange par défaut
     * - x-dead-letter-routing-key = nom de la DLQ → le message atterrit dans la DLQ
     *
     * Résultat : si un message échoue, il va automatiquement dans la DLQ
     */
    @Bean
    public Queue userRegisteredQueue() {
        return QueueBuilder
                .durable(userRegisteredQueue)
                .withArgument("x-dead-letter-exchange", "")  // exchange par défaut
                .withArgument("x-dead-letter-routing-key", userRegisteredDlq) // redirige vers DLQ
                .build();
    }

    // =====================================================================
    // 4. BINDING - La règle de routage
    // =====================================================================

    /**
     * Lie l'exchange à la queue avec la routing key
     *
     * En français : "Quand un message arrive sur l'exchange 'auth.events'
     * avec la routing key 'auth.user-registered', envoie-le dans la queue
     * 'notification.user-registered'"
     */
    @Bean
    public Binding userRegisteredBinding(Queue userRegisteredQueue, TopicExchange authExchange) {
        return BindingBuilder
                .bind(userRegisteredQueue)
                .to(authExchange)
                .with(userRegisteredRoutingKey);
    }

    // =====================================================================
    // 5. CONVERTISSEUR JSON - Pour envoyer/recevoir des objets Java en JSON
    // =====================================================================

    /**
     * Convertisseur de messages Java ↔ JSON
     *
     * Sans ce bean, RabbitMQ enverrait les objets en binaire Java (illisible).
     * Avec Jackson2JsonMessageConverter :
     * - À l'envoi : l'objet Java UserRegisteredEvent est converti en JSON
     * - À la réception : le JSON est reconverti en objet Java
     *
     * C'est ce qui permet de voir les messages lisibles dans le panneau RabbitMQ
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
