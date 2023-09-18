package com.example.bk.consumer;

import com.example.bk.Customer;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.kafka.KafkaItemReader;
import org.springframework.batch.item.kafka.builder.KafkaItemReaderBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;

import java.util.List;
import java.util.Properties;

@Log4j2
@SpringBootApplication
@RequiredArgsConstructor
@EnableBatchProcessing
public class ConsumerApplication {


	public static void main(String args[]) {
		SpringApplication.run(ConsumerApplication.class, args);
	}


	private final KafkaProperties properties;
	private final StepBuilderFactory stepBuilderFactory;
	private final JobBuilderFactory jobBuilderFactory;

	@Bean
	Job job() {
		return jobBuilderFactory.get("job")
			.incrementer(new RunIdIncrementer())
			.start(start())
			.build();

	}

	@Bean
	KafkaItemReader<Long, Customer> kafkaItemReader() {
		var props = new Properties();
		props.putAll(this.properties.buildConsumerProperties());

		return new KafkaItemReaderBuilder<Long, Customer>()
			.partitions(getPartitiions())
			.consumerProperties(props)
			.name("customers-reader")
			.saveState(true)
			.topic("customerskk")
			.build();
	}

	@Bean
	Step start() {
		var writer = new ItemWriter<Customer>() {
			@Override
			public void write(List<? extends Customer> items) throws Exception {
				items.forEach(it -> log.info("new customer: " + it));
			}
		};
		return stepBuilderFactory
			.get("step")
			.<Customer, Customer>chunk(10)
			.writer(writer)
			.reader(kafkaItemReader())
			.build();
	}

	@Bean
	public int getPartitiions(){
		Properties configProperties = new Properties();
		configProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,"localhost:9092");
		configProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,"org.apache.kafka.common.serialization.ByteArraySerializer");
		configProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,"org.apache.kafka.common.serialization.StringSerializer");
		org.apache.kafka.clients.producer.Producer producer = new KafkaProducer(configProperties);
		//producer.partitionsFor("customerskk");
		return producer.partitionsFor("customerskk").size()-1;
	}
}
