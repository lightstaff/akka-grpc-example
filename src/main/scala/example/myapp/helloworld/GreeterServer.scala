package example.myapp.helloworld

import scala.concurrent.{ExecutionContext, Future}

import akka.actor.ActorSystem
import akka.http.scaladsl.UseHttp2.Always
import akka.http.scaladsl.{Http, HttpConnectionContext}
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.{ActorMaterializer, Materializer}
import com.typesafe.config.ConfigFactory
import example.myapp.helloworld.grpc.GreeterServiceHandler

object GreeterServer {

  def main(args: Array[String]): Unit = {

    // http2をon 必須！！
    val conf =
      ConfigFactory
        .parseString("akka.http.server.preview.enable-http2 = on")
        .withFallback(ConfigFactory.defaultApplication())

    val system = ActorSystem("HelloWorld", conf)

    new GreeterServer(system).run()
  }
}

class GreeterServer(system: ActorSystem) {

  def run(): Future[Http.ServerBinding] = {
    implicit val sys: ActorSystem = system
    implicit val mat: Materializer = ActorMaterializer()
    implicit val ec: ExecutionContext = sys.dispatcher

    // サービスを生成
    val service: HttpRequest => Future[HttpResponse] = GreeterServiceHandler(
      new GreeterServiceImpl(mat))

    // akka-httpでバインド
    val bound = Http().bindAndHandleAsync(
      service,
      interface = "127.0.0.1",
      port = 18080,
      connectionContext = HttpConnectionContext(http2 = Always))

    bound.foreach { binding =>
      println(s"gRPC server bound to: ${binding.localAddress}")
    }

    bound
  }
}
