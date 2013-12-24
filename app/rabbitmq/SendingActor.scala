package rabbitmq

import com.rabbitmq.client.Channel
import play.api.Logger
import akka.actor.Actor

class SendingActor(channel: Channel, queue: String) extends Actor {
  def receive = {
    case some:String => {
      val msg = (some + " : " + System.currentTimeMillis());
      channel.basicPublish("", queue, null, msg.getBytes());
      Logger.info(msg);
    }
    case _ => {}
  }
}