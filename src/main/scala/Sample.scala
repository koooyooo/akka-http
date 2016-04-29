package main.scala

import akka.stream.scaladsl.{Flow, GraphDSL, RunnableGraph, Sink, Source}
import akka._
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape, FlowShape}

/**
  * Created by koyo on 2016/04/30.
  */
object Sample {
  def main(args: Array[String]) {
    implicit val actor = ActorSystem("my-actor")
    implicit val mater = ActorMaterializer()

    val graph = GraphDSL.create() {implicit builder =>
      import GraphDSL.Implicits._

      val flw1 = builder.add(Flow[Int].map{x: Int => x * 10})
      val flw2 = builder.add(Flow[Int].map{x: Int => println(x); x + 1 })

      flw1 ~> flw2

      FlowShape[Int, Int](flw1.in, flw2.out)
    }

    Source(1 to 5).via(graph).to(Sink.foreach(println(_))).run()



  }
}
