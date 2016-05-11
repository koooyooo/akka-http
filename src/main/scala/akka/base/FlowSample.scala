package akka.base

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, FlowShape}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge, Sink, Source, Zip}

/**
  * Created by koyo on 2016/05/12.
  */
object FlowSample {

  def main(args: Array[String]): Unit = {
    val target = 3
    target match {
      case 1 => basicGraphFlow()
      case 2 => twoSourceFlow()
      case 3 => feedbackFlow()
    }
  }


  /**
    * 基本的なフローを実現
    *
    * @return
    */
  def basicGraphFlow(): Unit = {
    implicit val actor = ActorSystem("my-actor")
    implicit val mater = ActorMaterializer()

    val source = Source(1 to 5)
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
    source.via(graph).to(Sink.foreach(println)).run
  }

  def twoSourceFlow(): Unit = {
    implicit val actor = ActorSystem("my-actor")
    implicit val mater = ActorMaterializer()

    val source = Source(1 to 5)
    val graph = GraphDSL.create() {implicit builder =>
      import GraphDSL.Implicits._

      val flow1 = builder.add(Flow[Int].map{x: Int => x})
      val src1 = builder.add(Source(List("aaa","bbb")))
      val zip = builder.add(Zip[Int, String])
      val flow2 = builder.add(Flow[(Int, String)].map{x: (Int, String) => x._1.toString + x._2})

        flow1 ~> zip.in0
        src1  ~> zip.in1

        zip.out ~> flow2

      FlowShape[Int, String](flow1.in, flow2.out)
    }
    source.via(graph).to(Sink.foreach(println)).run
  }

  def feedbackFlow(): Unit = {
    implicit val actor = ActorSystem("my-actor")
    implicit val mater = ActorMaterializer()

    val source = Source(1 to 2)
    val graph = GraphDSL.create() {implicit builder =>
      import GraphDSL.Implicits._

      val flow1 = builder.add(Flow[Int].map{x: Int => x})
      val flow2 = builder.add(Flow[Int].map{x: Int => x})
      val bc    = builder.add(Broadcast[Int](2))
      val sink1 = builder.add(Sink.foreach((x:Int) => println(s"($x)")))
      val src1 = builder.add(Source(List(10, 20, 30, 40, 50)))
      val zip = builder.add(Zip[Int, Int])
      val flow3 = builder.add(Flow[(Int, Int)].map{x: (Int, Int) => x._1 + x._2})

      flow1 ~> bc ~> sink1
               bc ~> flow2 ~> zip.in0
                     src1  ~> zip.in1
                              zip.out ~> flow3

      FlowShape[Int, Int](flow1.in, flow3.out)
    }
    source.via(graph).to(Sink.foreach(println)).run
  }
}
