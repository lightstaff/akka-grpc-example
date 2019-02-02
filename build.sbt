name := "akka-grpc-example"

version := "0.1"

scalaVersion := "2.12.8"

// akka-grpcプラグインを適用
enablePlugins(AkkaGrpcPlugin)

// JavaAgentプラグインを適用
enablePlugins(JavaAgent)

// JavaAgentにALPNを追加
javaAgents += "org.mortbay.jetty.alpn" % "jetty-alpn-agent" % "2.0.9" % "runtime;test"