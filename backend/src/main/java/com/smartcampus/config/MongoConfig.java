package com.smartcampus.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

/** Enables @CreatedDate auditing on documents. */
@Configuration
@EnableMongoAuditing
public class MongoConfig {
}
