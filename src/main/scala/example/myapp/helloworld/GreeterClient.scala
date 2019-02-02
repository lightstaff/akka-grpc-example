package example.myapp.helloworld

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.grpc.GrpcClientSettings
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import example.myapp.helloworld.grpc.{
  GreeterService,
  GreeterServiceClient,
  HelloReply,
  HelloRequest
}

object GreeterClient {

  def main(args: Array[String]): Unit = {
    implicit val sys: ActorSystem = ActorSystem("HelloWorldClient")
    implicit val mat: ActorMaterializer = ActorMaterializer()
    implicit val ec: ExecutionContext = sys.dispatcher

    // Walkthroughから生成方法を変更
    val clientSettings = GrpcClientSettings
      .connectToServiceAt("localhost", 18080)

    // clientを生成
    val client: GreeterService = GreeterServiceClient(clientSettings)

    // 下記メソッドからサービスを呼び出し
    runSingleRequestReplyExample()
    runStreamingRequestExample()
    runStreamingReplyExample()
    runStreamingRequestReplyExample()

    // 用も無く無限に呼び出してるのでコメントアウト
    // sys.scheduler.schedule(1.second, 1.second) {
    //   runSingleRequestReplyExample()
    // }

    // 単発リクエスト → 単発リプライ
    def runSingleRequestReplyExample(): Unit = {
      sys.log.info("Performing request")

      val reply = client.sayHello(HelloRequest("Alice"))

      reply.onComplete {
        case Success(msg) =>
          println(s"got single reply: $msg")
        case Failure(e) =>
          println(s"Error sayHello: $e")
      }
    }

    // ストリームリクエスト → 単発リプライ
    def runStreamingRequestExample(): Unit = {
      val requests = List("Alice", "Bob", "Peter").map(HelloRequest.apply)

      // リクエストとストリームとして送信
      val reply = client.itKeepsTalking(Source(requests))

      reply.onComplete {
        case Success(msg) =>
          println(s"got single reply for streaming requests: $msg")
        case Failure(e) =>
          println(s"Error streamingRequest: $e")
      }
    }

    // 単発リプライ → ストリームリクエスト
    def runStreamingReplyExample(): Unit = {
      // サービスから戻り値ストリーム
      val responseStream = client.itKeepsReplying(HelloRequest("Alice"))

      // サーバーからの送信が終わったらDone
      val done: Future[Done] =
        responseStream.runForeach(reply =>
          println(s"got streaming reply: ${reply.message}"))

      done.onComplete {
        case Success(_) =>
          println("streamingReply done")
        case Failure(e) =>
          println(s"Error streamingReply: $e")
      }
    }

    // ストリームリクエスト → ストリームリプライ
    def runStreamingRequestReplyExample(): Unit = {
      // 一秒に一回×10リクエスト
      val requestStream: Source[HelloRequest, NotUsed] = Source
        .tick(100.millis, 1.second, "tick")
        .zipWithIndex
        .map { case (_, i) => i }
        .map(i => HelloRequest(s"Alice-$i"))
        .take(10)
        .mapMaterializedValue(_ => NotUsed)

      // 戻り値ストリーム
      val responseStream: Source[HelloReply, NotUsed] =
        client.streamHellos(requestStream)

      // サーバーからの送信が終わったらDone
      val done: Future[Done] = responseStream.runForeach(reply =>
        println(s"got streaming reply: ${reply.message}"))

      done.onComplete {
        case Success(_) =>
          println("streamingRequestReply done")
        case Failure(e) =>
          println(s"Error streamingRequestReply: $e")
      }
    }
  }
}
