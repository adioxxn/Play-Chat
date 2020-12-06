package actors


import akka.actor.{Actor, ActorRef, Props}
import scala.collection.mutable.Queue
import scala.collection.mutable

class ChatManager extends Actor{

  private var chatters = List.empty[(ActorRef,String)]
  private var freeWorkers = List.empty[ActorRef]
  private var busyWorkers = List.empty[ActorRef]
  private var workqueue = Queue.empty[(ActorRef,String)]

  for (i <-0 to 4){
    freeWorkers ::= context.actorOf(Props(new ChatWorker()))
  }


  import ChatManager._
  def receive = {
    case NewChatter(chatter,name) => chatters ::= (chatter,name)
      println("Got message "+name)
    case Done =>
      if(this.workqueue.size>0){
        val work = workqueue.dequeue

        sender() ! ChatWorker.Message(work._2,chatters,work._1)
      }
      else{
        freeWorkers ::= sender()
        val a = busyWorkers.indexOf(sender())
        busyWorkers.drop(a)
      }

    case Message(msg) =>
      if (this.workqueue.size>1000){
        sender() ! ChatActor.Many
      }
      else{

        if (freeWorkers.size>0){
//          this.workqueue += ((sender(),"testing"))
          busyWorkers ::= freeWorkers.head
          freeWorkers=freeWorkers.drop(1)
          busyWorkers.head ! ChatWorker.Message(msg,chatters,sender())
        }
        else{
          workqueue.enqueue((sender(),msg))
        }
      }


    case m => println("Unhandled")
  }

}

object ChatManager{
  case class NewChatter(chatter: ActorRef, name:String)
  case class Message(msg: String)
  case class Login(name: String, password: String)
  case object Done
}
