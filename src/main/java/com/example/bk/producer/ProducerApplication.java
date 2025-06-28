package com.example.bk.producer;

import com.example.bk.Customer;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemReader;

import org.springframework.batch.item.json.JacksonJsonObjectReader;
import org.springframework.batch.item.json.JsonItemReader;
import org.springframework.batch.item.json.builder.JsonItemReaderBuilder;
import org.springframework.batch.item.kafka.KafkaItemWriter;
import org.springframework.batch.item.kafka.builder.KafkaItemWriterBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.PathResource;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.concurrent.atomic.AtomicLong;

@EnableBatchProcessing
@SpringBootApplication
@RequiredArgsConstructor
public class ProducerApplication {

	public static void main(String args[]) {
		SpringApplication.run(ProducerApplication.class, args);
	}

	private final JobBuilderFactory jobBuilderFactory;
	private final StepBuilderFactory stepBuilderFactory;
	private final KafkaTemplate<Long, Customer> template;

	@Bean
	Job job() {
		return this.jobBuilderFactory
			.get("job")
			.start(start())
			.incrementer(new RunIdIncrementer())
			.build();
	}

	@Bean
	KafkaItemWriter<Long, Customer> kafkaItemWriter() {
		return new KafkaItemWriterBuilder<Long, Customer>()
			.kafkaTemplate(template)
			.itemKeyMapper(Customer::getId)
			.build();
	}

	@Bean
	Step start() {
		return this.stepBuilderFactory
			.get("s1")
			.<Customer, Customer>chunk(1000)
			.reader(jsonItemReaderP())
			.writer(kafkaItemWriter())
			.build();
	}

	@Bean
	public JsonItemReader<Customer> jsonItemReaderP() {
		return new JsonItemReaderBuilder<Customer>()
				.jsonObjectReader(new JacksonJsonObjectReader<>(Customer.class))
				.resource(new PathResource("peter/peter.json"))
				.name("studentJsonItemReader")
				.build();
	}
}