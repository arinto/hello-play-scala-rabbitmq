package rabbitmq

import queue.RabbitMQConnection
import utils.Config
import java.util.concurrent.TimeUnit
import play.api.libs.concurrent.Akka
import play.api.Play.current
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.Props
import play.api.Logger
import com.rabbitmq.client.Channel

object Sender { 

  def startSending() = {
    val connection = RabbitMQConnection.getConnection;
    val sendingChannel = connection.createChannel();

    sendingChannel.queueDeclare(Config.RABBITMQ_QUEUE, false, false, false, null);
    
    Akka.system.scheduler.schedule(FiniteDuration(2, TimeUnit.SECONDS)
        , FiniteDuration(1, TimeUnit.SECONDS)
        , Akka.system.actorOf(
            Props(new SendingActor(channel = sendingChannel, queue = Config.RABBITMQ_QUEUE)))
        ,"MSG to Queue");
    
    val callback1 = (x: String) => Logger.info("Received on queue callback 1: " + x);
    setupListener(connection.createChannel(), Config.RABBITMQ_QUEUE, callback1);
    
    val callback2 = (x: String) => Logger.info("Received on queue callback 2: " + x);
    
    setupListener(connection.createChannel(), Config.RABBITMQ_QUEUE, callback2);
  }
  
  private def setupListener(receivingChannel: Channel, queue: String, f: (String) => Any){
    Akka.system.scheduler.scheduleOnce(FiniteDuration(2, TimeUnit.SECONDS)
        , Akka.system.actorOf(
            Props(new ListeningActor(receivingChannel, queue, f)))
        , "");
  }
}