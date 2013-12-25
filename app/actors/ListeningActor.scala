package actors

import akka.actor.Actor
import com.rabbitmq.client.QueueingConsumer
import com.rabbitmq.client.Channel
import akka.actor.Props

class ListeningActor(channel: Channel, queue: String, f: (String) => Any) extends Actor {
  def receive = {
    case _ => startReceiving
  }
  
  def startReceiving = {
    val consumer = new QueueingConsumer(channel);
    channel.basicConsume(queue, true, consumer);
    
    while(true){
      val delivery = consumer.nextDelivery();
      val msg = new String(delivery.getBody());
      
      context.actorOf(Props(new Actor{
        def receive = {
          case some: String => f(some);
        }
      })) ! msg
    }
  }

}