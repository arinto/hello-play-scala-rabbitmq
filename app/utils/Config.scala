package utils

import com.typesafe.config.ConfigFactory

object Config {
	val RABBITMQ_HOST = ConfigFactory.load().getString("rabbitmq.host");
	val RABBITMQ_QUEUE = ConfigFactory.load().getString("rabbitmq.queue");
	val RABBITMQ_EXCHANGEE = ConfigFactory.load().getString("rabbitmq.exchange");
}