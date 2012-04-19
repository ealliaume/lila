package lila
package lobby

import akka.actor._
import akka.pattern.ask
import akka.util.duration._
import akka.util.Timeout
import play.api.libs.json._
import play.api.libs.iteratee._
import play.api.libs.concurrent._
import scalaz.effects._

import RichJs._

final class Socket(hub: ActorRef) {

  implicit val timeout = Timeout(1 second)

  def join(
    uid: String,
    version: Int,
    hook: Option[String]): SocketPromise =
    (hub ? Join(uid, version, hook)).asPromise map {
      case Connected(channel) ⇒
        val iteratee = Iteratee.foreach[JsValue] { e ⇒
          e str "t" match {
            case Some("talk") ⇒ for {
              data ← e obj "d"
              txt ← data str "txt"
              username ← data str "u"
            } hub ! Talk(txt, username)
            case _ ⇒
          }
        } mapDone { _ ⇒
          hub ! Quit(uid)
        }
        (iteratee, channel)
    }

  def addEntry(entry: model.Entry): IO[Unit] = io {
    hub ! Entry(entry)
  }

  def removeHook(hook: model.Hook): IO[Unit] = io {
    hub ! RemoveHook(hook)
  }

  def addHook(hook: model.Hook): IO[Unit] = io {
    hub ! AddHook(hook)
  }

  def biteHook(hook: model.Hook, game: model.DbGame): IO[Unit] = io {
    hub ! BiteHook(hook, game)
  }
}