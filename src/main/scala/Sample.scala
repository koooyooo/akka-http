package main.scala

import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge, Sink, Source}
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

      val flw1 = builder.add(Flow[Int].map{x: Int => x })
      val flw2 = builder.add(Flow[Int].map{x: Int => x * 10  })
      val flw3 = builder.add(Flow[Int].map{x: Int => x * 100 })
      val flw4 = builder.add(Flow[Int].map{x: Int => x })
      val bc = builder.add(Broadcast[Int](2))
      val mg = builder.add(Merge[Int](2))

      flw1 ~> bc ~> flw2 ~> mg ~> flw4
              bc ~> flw3 ~> mg

      FlowShape[Int, Int](flw1.in, flw4.out)
    }

    Source(1 to 5).via(graph).to(Sink.foreach(println(_))).run()



  }
}
