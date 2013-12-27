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
  
  val connection = RabbitMQConnection.getConnection;
  val sendingChannel1 = connection.createChannel();
  val listenChannel1 = connection.createChannel();
  val listenChannel2 = connection.createChannel();
  val sendingChannel2 = connection.createChannel();
  val listenChannel3 = connection.createChannel();
  val listenChannel4 = connection.createChannel();
  
  val channels = Set(sendingChannel1, sendingChannel2, listenChannel1, listenChannel2, listenChannel3, listenChannel4)
  
  def startSending() = {
    sendingChannel1.queueDeclare(Config.RABBITMQ_QUEUE, false, false, false, null);
    
    Akka.system.scheduler.schedule(FiniteDuration(2, TimeUnit.SECONDS)
        , FiniteDuration(1, TimeUnit.SECONDS)
        , Akka.system.actorOf(
            Props(new SendingActor(channel = sendingChannel1, queue = Config.RABBITMQ_QUEUE)))
        ,"MSG to Queue");
    
    val callback1 = (x: String) => Logger.info("Received on queue callback 1: " + x);
    setupListener(listenChannel1, Config.RABBITMQ_QUEUE, callback1);
    
    val callback2 = (x: String) => Logger.info("Received on queue callback 2: " + x);
    setupListener(listenChannel2, Config.RABBITMQ_QUEUE, callback2);

    sendingChannel2.exchangeDeclare(Config.RABBITMQ_EXCHANGEE, "fanout");
    
    val callback3 = (x: String) => Logger.info("Received on exchange callback 3: " + x);
    setupListener(listenChannel3, listenChannel3.queueDeclare().getQueue(), Config.RABBITMQ_EXCHANGEE, callback3);
    
    val callback4 = (x: String) => Logger.info("Received on exchange callback 4: " + x);
    setupListener(listenChannel4, listenChannel4.queueDeclare().getQueue(), Config.RABBITMQ_EXCHANGEE, callback4);
    
    Akka.system.scheduler.schedule(FiniteDuration(2, TimeUnit.SECONDS)
        , FiniteDuration(2, TimeUnit.SECONDS)
        , Akka.system.actorOf(
            Props(new PublishingActor(channel = sendingChannel1, exchange = Config.RABBITMQ_EXCHANGEE)))
        ,"MSG to Exchange");
  }
  
  def stopEverything() = {
    channels.foreach(channel => channel.close())
    connection.close()
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