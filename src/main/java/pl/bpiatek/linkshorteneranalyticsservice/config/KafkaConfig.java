package pl.bpiatek.linkshorteneranalyticsservice.config;

import io.confluent.kafka.serializers.protobuf.KafkaProtobufDeserializer;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufDeserializerConfig;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufSerializer;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufSerializerConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
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
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import pl.bpiatek.contracts.analytics.AnalyticsEventProto;
import pl.bpiatek.contracts.link.LinkClickEventProto.LinkClickEvent;
import pl.bpiatek.contracts.link.LinkLifecycleEventProto.LinkLifecycleEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

@Configuration
@EnableKafka
class KafkaConfig {

    private final KafkaProperties kafkaProperties;

     KafkaConfig(KafkaProperties kafkaProperties) {
        this.kafkaProperties = kafkaProperties;
    }

    @Bean
    ConsumerFactory<String, LinkClickEvent> linkClickEventConsumerFactory() {
        Map<String, Object> props = baseConsumerProperties();
        props.put(KafkaProtobufDeserializerConfig.SPECIFIC_PROTOBUF_VALUE_TYPE, LinkClickEvent.class);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, LinkClickEvent> linkClickEventContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, LinkClickEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(linkClickEventConsumerFactory());
        factory.getContainerProperties()
                .setListenerTaskExecutor(new ConcurrentTaskExecutor(
                        Executors.newThreadPerTaskExecutor(Thread.ofVirtual().factory())
                ));
        return factory;
    }

    @Bean
    ConsumerFactory<String, LinkLifecycleEvent> linkLifecycleEventConsumerFactory() {
        Map<String, Object> props = baseConsumerProperties();
        props.put(KafkaProtobufDeserializerConfig.SPECIFIC_PROTOBUF_VALUE_TYPE, LinkLifecycleEvent.class.getName());
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, LinkLifecycleEvent> linkLifecycleEventContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, LinkLifecycleEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(linkLifecycleEventConsumerFactory());
        factory.getContainerProperties()
                .setListenerTaskExecutor(new ConcurrentTaskExecutor(
                        Executors.newThreadPerTaskExecutor(Thread.ofVirtual().factory())
                ));
        return factory;
    }

    private Map<String, Object> baseConsumerProperties() {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildConsumerProperties(null));
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaProtobufDeserializer.class);
        putSchemaRegistry(props);

        return props;
    }

    @Bean
    ProducerFactory<String, AnalyticsEventProto.LinkClickEnrichedEvent> enrichedClickEventProducerFactory() {
        return new DefaultKafkaProducerFactory<>(baseProducerProperties());
    }

    @Bean
    KafkaTemplate<String, AnalyticsEventProto.LinkClickEnrichedEvent> enrichedClickEventKafkaTemplate() {
        return new KafkaTemplate<>(enrichedClickEventProducerFactory());
    }

    private Map<String, Object> baseProducerProperties() {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildProducerProperties(null));
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaProtobufSerializer.class);
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