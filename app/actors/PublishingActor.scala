package actors

import akka.actor.Actor
import com.rabbitmq.client.Channel
import play.api.Logger

class PublishingActor(channel: Channel, exchange: String) extends Actor {
	
    def receive = {
      case some: String => {
        val msg = (some + " : " + System.currentTimeMillis());
        channel.basicPublish(exchange, "", null, msg.getBytes());
        Logger.info(msg)
      }
      case _ => {}
	}
}