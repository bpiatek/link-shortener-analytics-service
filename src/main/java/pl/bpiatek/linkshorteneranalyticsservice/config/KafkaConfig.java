package pl.bpiatek.linkshorteneranalyticsservice.config;

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufDeserializer;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufDeserializerConfig;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufSerializer;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufSerializerConfig;
import io.micrometer.observation.ObservationRegistry;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;
import pl.bpiatek.contracts.analytics.AnalyticsEventProto;
import pl.bpiatek.contracts.link.LinkClickEventProto.LinkClickEvent;
import pl.bpiatek.contracts.link.LinkLifecycleEventProto.LinkLifecycleEvent;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
class KafkaConfig {

    private final KafkaProperties kafkaProperties;

    KafkaConfig(KafkaProperties kafkaProperties) {
        this.kafkaProperties = kafkaProperties;
    }

    // =================================================================================
    // ERROR HANDLER (Blocking Retry)
    // =================================================================================
    @Bean
    public DefaultErrorHandler errorHandler() {
        // Wait 1 second between attempts. Try 5 times max.
        return new DefaultErrorHandler(new FixedBackOff(1000L, 5));
    }

    // =================================================================================
    // CONSUMER 1: LinkClickEvent
    // =================================================================================

    @Bean
    ConsumerFactory<String, LinkClickEvent> linkClickEventConsumerFactory() {
        Map<String, Object> props = baseConsumerProperties();
        props.put(KafkaProtobufDeserializerConfig.SPECIFIC_PROTOBUF_VALUE_TYPE, LinkClickEvent.class);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, LinkClickEvent> linkClickEventContainerFactory(
            ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
            ConsumerFactory<String, LinkClickEvent> linkClickEventConsumerFactory) {

        ConcurrentKafkaListenerContainerFactory<String, LinkClickEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        configurer.configure(
                (ConcurrentKafkaListenerContainerFactory) factory,
                (ConsumerFactory) linkClickEventConsumerFactory
        );

        return factory;
    }

    // =================================================================================
    // CONSUMER 2: LinkLifecycleEvent
    // =================================================================================

    @Bean
    ConsumerFactory<String, LinkLifecycleEvent> linkLifecycleEventConsumerFactory() {
        Map<String, Object> props = baseConsumerProperties();
        props.put(KafkaProtobufDeserializerConfig.SPECIFIC_PROTOBUF_VALUE_TYPE, LinkLifecycleEvent.class);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, LinkLifecycleEvent> linkLifecycleEventContainerFactory(
            ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
            ConsumerFactory<String, LinkLifecycleEvent> linkLifecycleEventConsumerFactory) {

        ConcurrentKafkaListenerContainerFactory<String, LinkLifecycleEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        configurer.configure(
                (ConcurrentKafkaListenerContainerFactory) factory,
                (ConsumerFactory) linkLifecycleEventConsumerFactory
        );

        return factory;
    }

    private Map<String, Object> baseConsumerProperties() {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildConsumerProperties(null));
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaProtobufDeserializer.class);
        putSchemaRegistry(props);
        return props;
    }

    // =================================================================================
    // PRODUCER: LinkClickEnrichedEvent
    // =================================================================================

    @Bean
    ProducerFactory<String, AnalyticsEventProto.LinkClickEnrichedEvent> enrichedClickEventProducerFactory() {
        return new DefaultKafkaProducerFactory<>(baseProducerProperties());
    }

    @Bean
    KafkaTemplate<String, AnalyticsEventProto.LinkClickEnrichedEvent> enrichedClickEventKafkaTemplate(
            ObservationRegistry observationRegistry) {
        var template = new KafkaTemplate<>(enrichedClickEventProducerFactory());
        template.setObservationEnabled(true);
        template.setObservationRegistry(observationRegistry);
        return template;
    }

    private Map<String, Object> baseProducerProperties() {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildProducerProperties(null));
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaProtobufSerializer.class);
        props.put(AbstractKafkaSchemaSerDeConfig.AUTO_REGISTER_SCHEMAS, false);
        props.put(AbstractKafkaSchemaSerDeConfig.USE_LATEST_VERSION, true);
        putSchemaRegistry(props);
        return props;
    }

    private void putSchemaRegistry(Map<String, Object> props) {
        var schemaRegistryUrl = kafkaProperties.getProperties().get("schema.registry.url");
        if (schemaRegistryUrl != null) {
            props.put(KafkaProtobufSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);
        }
    }
}