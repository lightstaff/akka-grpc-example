package example.myapp.helloworld

import scala.concurrent.Future

import akka.NotUsed
import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source

import example.myapp.helloworld.grpc._

// GreeterServerの実装
class GreeterServiceImpl(materializer: Materializer) extends GreeterService {

  import materializer.executionContext
  private implicit val mat: Materializer = materializer

  // 単発リクエスト → 単発リプライ
  override def sayHello(in: HelloRequest): Future[HelloReply] = {
    println(s"sayHello to ${in.name}")

    // そのまんま
    Future.successful(HelloReply(s"Hello, ${in.name}"))
  }

  // ストリームリクエスト → 単発リプライ
  override def itKeepsTalking(
      in: Source[HelloRequest, NotUsed]): Future[HelloReply] = {
    println(s"sayHello to in stream...")

    // SourceをSinkで受けて、リクエストのnameを結合
    in.runWith(Sink.seq)
      .map(elements =>
        HelloReply(s"Hello, ${elements.map(_.name).mkString(", ")}"))
  }

  // 単発リプライ → ストリームリクエスト
  override def itKeepsReplying(
      in: HelloRequest): Source[HelloReply, NotUsed] = {
    println(s"sayHello to ${in.name} with stream of chars...")

    // リクエストのnameをcharをSource化して流す
    Source(s"Hello, ${in.name}".toList).map(character =>
      HelloReply(character.toString))
  }

  // ストリームリクエスト → ストリームリプライ
  override def streamHellos(
      in: Source[HelloRequest, NotUsed]): Source[HelloReply, NotUsed] = {
    println(s"sayHello to stream...")

    // SourceからSource
    in.map(request => HelloReply(s"Hello, s${request.name}"))
  }
}
