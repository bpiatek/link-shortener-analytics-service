package pl.bpiatek.linkshorteneranalyticsservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.redpanda.RedpandaContainer;
import org.testcontainers.utility.DockerImageName;
import pl.bpiatek.contracts.analytics.AnalyticsEventProto;
import pl.bpiatek.contracts.link.LinkLifecycleEventProto;
import pl.bpiatek.linkshorteneranalyticsservice.click.TestKafkaConsumer;
import org.springframework.kafka.test.utils.ContainerTestUtils;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(IntegrationTest.TestConfig.class)
public abstract class IntegrationTest {

    @ServiceConnection
    public static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @ServiceConnection
    public static final RedpandaContainer redpanda = new RedpandaContainer(
            DockerImageName.parse("docker.redpanda.com/redpandadata/redpanda:v24.1.4")
    );

    static {
        postgres.start();
        redpanda.start();
    }

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected TestKafkaConsumer<LinkLifecycleEventProto.LinkLifecycleEvent> testLifecycleConsumer;

    @Autowired
    protected TestKafkaConsumer<AnalyticsEventProto.LinkClickEnrichedEvent> testEnrichedClickConsumer;

    @DynamicPropertySource
    static void infrastructureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", redpanda::getBootstrapServers);
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("TRUNCATE TABLE clicks, analytics_links RESTART IDENTITY CASCADE");
        testLifecycleConsumer.reset();
        testEnrichedClickConsumer.reset();
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        static BeanPostProcessor confluentSchemaRegistryFixer() {
            return new BeanPostProcessor() {
                @Override
                public Object postProcessBeforeInitialization(Object bean, String beanName) {

                    // Masterful use of Java 21 Pattern Matching to cleanly route configuration
                    switch (bean) {
                        case DefaultKafkaProducerFactory<?, ?> producerFactory -> {
                            var producerProps = java.util.Map.<String, Object>of(
                                    "schema.registry.url", "mock://test-registry",
                                    "auto.register.schemas", true,
                                    "use.latest.version", false
                            );
                            producerFactory.updateConfigs(producerProps);
                        }
                        case DefaultKafkaConsumerFactory<?, ?> consumerFactory -> {
                            var consumerProps = java.util.Map.<String, Object>of(
                                    "schema.registry.url", "mock://test-registry",
                                    "derive.type", true
                            );
                            consumerFactory.updateConfigs(consumerProps);
                        }
                        default -> {
                            // No-op for the thousands of other beans Spring initializes
                        }
                    }

                    return bean;
                }
            };
        }

        @Bean
        TestKafkaConsumer<LinkLifecycleEventProto.LinkLifecycleEvent> testLifecycleConsumer() {
            return new TestKafkaConsumer<>();
        }

        @Bean
        TestKafkaConsumer<AnalyticsEventProto.LinkClickEnrichedEvent> testEnrichedClickConsumer() {
            return new TestKafkaConsumer<>();
        }

        @KafkaListener(
                topics = "${topic.link.lifecycle}",
                groupId = "it-lifecycle-group-${random.uuid}"
        )
        void listenLifecycle(ConsumerRecord<String, LinkLifecycleEventProto.LinkLifecycleEvent> record) {
            testLifecycleConsumer().handle(record);
        }

        @KafkaListener(
                topics = "${topic.analytics.enriched}",
                groupId = "it-enriched-group-${random.uuid}"
        )
        void listenAnalyticsEnriched(ConsumerRecord<String, AnalyticsEventProto.LinkClickEnrichedEvent> record) {
            testEnrichedClickConsumer().handle(record);
        }

        @Bean
        NewTopic linkClicksTopic(@Value("${topic.link.clicks}") String topicName) {
            return TopicBuilder.name(topicName)
                    .partitions(1)
                    .replicas(1)
                    .build();
        }

        @Bean
        NewTopic linkLifecycleTopic(@Value("${topic.link.lifecycle}") String topicName) {
            return TopicBuilder.name(topicName)
                    .partitions(1)
                    .replicas(1)
                    .build();
        }

        @Bean
        NewTopic analyticsEnrichedTopic(@Value("${topic.analytics.enriched}") String topicName) {
            return TopicBuilder.name(topicName)
                    .partitions(1)
                    .replicas(1)
                    .build();
        }

        @Bean
        ApplicationRunner waitForKafkaAssignment(KafkaListenerEndpointRegistry registry) {
            return args ->
                    registry.getListenerContainers().forEach(container ->
                            ContainerTestUtils.waitForAssignment(container, 1));
        }
    }
}