//#full-example
package com.example

import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.SpawnProtocol
import akka.actor.typed.scaladsl.Behaviors
import akka.util.Timeout
import akka.actor.Status
import scala.util.Failure
import scala.util.Success
import akka.actor.typed.Dispatchers
import scala.concurrent.ExecutionContext
import akka.actor.typed.Props
import scala.concurrent.Future
import akka.stream._
import akka.stream.scaladsl._
import akka.NotUsed
import akka.util.ByteString
import java.nio.file.Paths
import akka.actor
import akka.Done
import scala.concurrent.duration._
import scala.util.Random
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.HttpMethods
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.ContentType
import akka.http.scaladsl.model.MediaTypes
import akka.http.scaladsl.model.HttpCharsets
import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.StatusCodes.OK
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import akka.stream.scaladsl.Framing

object Main extends App {

  def apply(): Behavior[SpawnProtocol.Command] = SpawnProtocol.apply()

  implicit val system = ActorSystem(Main(), "system")

  implicit val ex = system.classicSystem.dispatcher

  val connections: Source[Tcp.IncomingConnection, Future[Tcp.ServerBinding]] =
    Tcp(system).bind("127.0.0.1", 8888)

  val quiteFlow = Flow[String]
    .takeWhile(_.toLowerCase.trim != ":q")
    .concat(Source.single("BYE"))

  connections.runForeach { connection =>
    println(s"New connection from: ${connection.remoteAddress}")

    val echo: Flow[ByteString,ByteString,Future[Done]] = Flow[ByteString]
      .prepend(Source.single(ByteString("Hello !! \n")))
      .via(
        Framing.delimiter(
          ByteString("\n"),
          maximumFrameLength = 445,
          allowTruncation = true
        )
      )
      .map(_.utf8String)
      .alsoTo(Sink.foreach(println))
      .via(
        quiteFlow
      )
      .map(e => s">> $e \n")
      .map(ByteString(_))
      .watchTermination()(Keep.right)

    connection.handleWith(echo).onComplete {
      case Success(_) => println("stream completed successfully")
      case Failure(e) => println(e.getMessage)
    }
  }

}

object Manin2 extends App {

  def apply(): Behavior[SpawnProtocol.Command] = SpawnProtocol.apply()

  implicit val system = ActorSystem(Main(), "system")

  implicit val ex = system.classicSystem.dispatcher

  val connection: Flow[ByteString, ByteString, Future[Tcp.OutgoingConnection]] =
    Tcp(system).outgoingConnection("127.0.0.1", 8888)

  val x: Future[Done] = Source
    .repeat("Hello\n")
    .map(ByteString.apply)
    .throttle(1, FiniteDuration.apply(1, "second"))
    .via(connection)
    .run()

  x.onComplete {
    case Success(value) => println("colsoed......")
    case Failure(err)   => println(err.getMessage())
  }

}

object Manin3 extends App {

  def apply(): Behavior[SpawnProtocol.Command] = SpawnProtocol.apply()

  implicit val system = ActorSystem(Main(), "system")

  implicit val ex = system.classicSystem.dispatcher

  val connection: Flow[ByteString, ByteString, Future[Tcp.OutgoingConnection]] =
    Tcp(system).outgoingConnection("www.google.com", 80)

  val test: List[ByteString] =
    List("GET / HTTP/1.1", "Host: www.google.com", "\r\n").map(s ⇒
      ByteString(s + "\r\n")
    )

  val x: RunnableGraph[(Future[Tcp.OutgoingConnection], Future[Done])] =
    Source(test)
      .viaMat(connection)(Keep.right)
      .toMat(Sink.foreach(e => println(e.utf8String)))(Keep.both)

  val (res1, res2): (Future[Tcp.OutgoingConnection], Future[Done]) = x.run()

  res2.onComplete {
    case Success(value) => println("Connection colsoed......")
    case Failure(err)   => println(err.getMessage())
  }


}

object Manin4 extends App {

  def apply(): Behavior[SpawnProtocol.Command] = SpawnProtocol.apply()

  implicit val system = ActorSystem(Main(), "system")

  implicit val ex = system.classicSystem.dispatcher

  val connection: Flow[ByteString, ByteString, Future[Tcp.OutgoingConnection]] =
    Tcp(system).outgoingConnection("127.0.0.1", 8888)

  val x = Source.maybe[ByteString].via(connection).alsoTo(Sink.foreach(e => println(e.utf8String))).run()

  x.onComplete {
    case Success(value) => println("colsoed......")
    case Failure(err)   => println(err.getMessage())
  }

}
