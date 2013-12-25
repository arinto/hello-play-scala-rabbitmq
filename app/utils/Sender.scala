package utils

import play.api.libs.concurrent.Akka
import play.api.Logger
import play.api.Play.current
import akka.actor.Props

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.ExecutionContext.Implicits.global

import com.rabbitmq.client.Channel

import actors.ListeningActor
import actors.SendingActor
import actors.PublishingActor
import queue.RabbitMQConnection

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

    val sendingChannel2 = connection.createChannel();
    sendingChannel2.exchangeDeclare(Config.RABBITMQ_EXCHANGEE, "fanout");
    
    val callback3 = (x: String) => Logger.info("Received on exchange callback 3: " + x);
    val listenChannel3 = connection.createChannel();
    setupListener(listenChannel3, listenChannel3.queueDeclare().getQueue(), Config.RABBITMQ_EXCHANGEE, callback3);
    
    val callback4 = (x: String) => Logger.info("Received on exchange callback 4: " + x);
    val listenChannel4 = connection.createChannel();
    setupListener(listenChannel4, listenChannel4.queueDeclare().getQueue(), Config.RABBITMQ_EXCHANGEE, callback4);
    
    Akka.system.scheduler.schedule(FiniteDuration(2, TimeUnit.SECONDS)
        , FiniteDuration(2, TimeUnit.SECONDS)
        , Akka.system.actorOf(
            Props(new PublishingActor(channel = sendingChannel, exchange = Config.RABBITMQ_EXCHANGEE)))
        ,"MSG to Exchange");
  }
  
  private def setupListener(receivingChannel: Channel, queue: String, f: (String) => Any){
    Akka.system.scheduler.scheduleOnce(FiniteDuration(2, TimeUnit.SECONDS)
        , Akka.system.actorOf(
            Props(new ListeningActor(receivingChannel, queue, f)))
        , "");
  }
  
  private def setupListener(receivingChannel: Channel, queue: String, exchange: String, f: (String) => Any){
	receivingChannel.queueBind(queue, exchange, "");
    
    Akka.system.scheduler.scheduleOnce(FiniteDuration(2, TimeUnit.SECONDS)
        , Akka.system.actorOf(
            Props(new ListeningActor(receivingChannel, queue, f)))
        , "");
  }
}