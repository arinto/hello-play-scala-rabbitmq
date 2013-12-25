import play.api.mvc._
import play.api._
import utils.Sender

object Global extends GlobalSettings {
  
  override def onStart(app: Application){
    Sender.startSending
  }
 }