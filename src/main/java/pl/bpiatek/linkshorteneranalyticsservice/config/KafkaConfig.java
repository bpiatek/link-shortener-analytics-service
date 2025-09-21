package pl.bpiatek.linkshorteneranalyticsservice.config;

import io.confluent.kafka.serializers.protobuf.KafkaProtobufDeserializer;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufDeserializerConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import pl.bpiatek.contracts.link.LinkClickEventProto;

import java.util.Map;

@Configuration
@EnableKafka
class KafkaConfig {

    private final KafkaProperties kafkaProperties;

    public KafkaConfig(KafkaProperties kafkaProperties) {
        this.kafkaProperties = kafkaProperties;
    }

    @Bean
    public ConsumerFactory<String, LinkClickEventProto.LinkClickEvent> linkClickEventConsumerFactory() {
        Map<String, Object> props = kafkaProperties.buildConsumerProperties(null);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaProtobufDeserializer.class);
        props.put(KafkaProtobufDeserializerConfig.SPECIFIC_PROTOBUF_VALUE_TYPE, LinkClickEventProto.LinkClickEvent.class);
        putSchemaRegistryUrl(props);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    @Primary
    public ConcurrentKafkaListenerContainerFactory<String, LinkClickEventProto.LinkClickEvent> linkClickEventContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, LinkClickEventProto.LinkClickEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(linkClickEventConsumerFactory());
        return factory;
    }

    private void putSchemaRegistryUrl(Map<String, Object> props) {
        var registryUrl = kafkaProperties.getProperties().get("schema.registry.url");
        if (registryUrl != null) {
            props.put("schema.registry.url", registryUrl);
        }
    }
}