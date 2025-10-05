package pl.bpiatek.linkshorteneranalyticsservice.config;

import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import pl.bpiatek.contracts.link.LinkClickEventProto.LinkClickEvent;
import pl.bpiatek.contracts.link.LinkLifecycleEventProto.LinkLifecycleEvent;

@TestConfiguration
class KafkaTestProducersConfiguration {


    @Bean
    public KafkaTemplate<String, LinkClickEvent> rawClickEventProducer(KafkaProperties kafkaProperties) {
        var props = kafkaProperties.buildProducerProperties(null);
        ProducerFactory<String, LinkClickEvent> pf = new DefaultKafkaProducerFactory<>(props);
        return new KafkaTemplate<>(pf);
    }

    @Bean
    public KafkaTemplate<String, LinkLifecycleEvent> linkLifecycleEventProducer(KafkaProperties kafkaProperties) {
        var props = kafkaProperties.buildProducerProperties(null);
        ProducerFactory<String, LinkLifecycleEvent> pf = new DefaultKafkaProducerFactory<>(props);
        return new KafkaTemplate<>(pf);
    }
}
