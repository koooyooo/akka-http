package akka.base

import java.io.File
import java.util.concurrent.TimeUnit

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Broadcast, FileIO, Flow, GraphDSL, Keep, Merge, Sink, Source}
import akka.stream._
import akka.util.ByteString

import scala.concurrent.duration._
import scala.concurrent.Future
import scala.concurrent.duration.{DurationLong, FiniteDuration}

/**
  * Created by koyo on 2016/04/30.
  */
object Sample {

  implicit val actor = ActorSystem("my-actor")
  implicit val mater = ActorMaterializer()

  def main(args: Array[String]): Unit = {
    val target = 9
    target match {
      case 1 => basicGraphFlow()
      case 2 => reusablePieces()
      case 3 => timeBasedProcessing()
        // 以降、Flow.scala に定義されたメソッドの検証
      case 4 => collectProcessing()
      case 5 => scanProcessing()
      case 6 => foldProcessing()
      case 7 => reduceProcessing()
      case 8 => interspaceProcessing()
      case 9 => interspaceProcessing2()

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
    source.via(graph).to(Sink.foreach(println(_))).run()
  }

  /**
    * 1種類の Sourceを基に、複数のSinkを切り替えて使う部分を表現
    *
    * @return
    */
  def reusablePieces(): Unit = {
    implicit val actor = ActorSystem("my-actor")
    implicit val mater = ActorMaterializer()

    // Source
    val factorials: Source[Int, NotUsed]#Repr[BigInt] =
      Source(1 to 10).scan(BigInt(1)){ (acc, next) => acc * next}

    // Sink 標準出力
    val sink1: Sink[String, Future[Done]] = Sink.foreach(i => println(i))

    // Sink ファイル出力 (単体)
    val sink2: String => Sink[ByteString, Future[IOResult]] = fileName =>
      FileIO.toFile(new File(fileName))

    // Sink ファイル出力 (Mat合成)
    val sink3: String => Sink[String, Future[IOResult]] = fileName =>
      Flow[String]
        .map(s => ByteString(s + "\n"))
        .toMat(FileIO.toFile(new File(fileName)))(Keep.right) // toMatは(Flow+Sink)で合成したSinkを生成するもの

    // Sinkを切り替えながらの実行
    factorials.map(String.valueOf(_)).runWith(sink1)
    factorials.map(num => ByteString(s"$num\n")).runWith(sink2("factorials2.txt"))
    factorials.map(_.toString).runWith(sink3("factorials3.txt"))
  }

  /**
    * スロットリングを行うサンプル
    * 1to100 のインデックスを定義している。(実際に使われるのは先の1to10に対応した 10迄の値のみ)
    *
    * @return
    */
  def timeBasedProcessing(): Unit = {
    implicit val actor = ActorSystem("my-actor")
    implicit val mater = ActorMaterializer()

    val factorials: Source[Int, NotUsed]#Repr[BigInt] =
      Source(1 to 10).scan(BigInt(1)){ (acc, next) => acc * next}

    factorials.zipWith(Source(1 to 100))((num, index) => s"$index! = $num")
      .throttle(1, 1.second, 1, ThrottleMode.shaping)
        .runForeach(str => println(str))
  }

  // # emitとは何か
  // filter と collectは何処がどう違うのか？

  def collectProcessing(): Unit = {
    implicit val actor = ActorSystem("my-actor")
    implicit val mater = ActorMaterializer()

    // {}括弧でないと認識されない ()とは何処が違うのか？
    Source(1 to 10).collect{case i if i % 2 == 0 => i}.runWith(Sink.foreach(x => println(x)))
    // 2, 4, 6, 8, 10
  }

  /**
    * 初期値(0)をもとに、累積(acc)と次の値(next)を取り、各結果のリストを得る
    */
  def scanProcessing(): Unit = {
    implicit val actor = ActorSystem("my-actor")
    implicit val mater = ActorMaterializer()

    Source(1 to 10).scan(0)((acc, next) => acc + next).runWith(Sink.foreach(println))
    // 0, 1, 3, 6, 10, 15, 21, 28, 36, 45, 55
  }

  /**
    * 初期値(0)をもとに、累積(acc)と次の値(next)を取り、最終結果を単体で得る
    */
  def foldProcessing(): Unit = {
    implicit val actor = ActorSystem("my-actor")
    implicit val mater = ActorMaterializer()

    Source(1 to 10).fold(0)((acc, next) => acc + next).runWith(Sink.foreach(println))
    // 55
  }

  /**
    * 初期値を最初の要素とし、累積(acc)と次の値(next)を取り、最終結果を単体で得る
    */
  def reduceProcessing(): Unit = {
    implicit val actor = ActorSystem("my-actor")
    implicit val mater = ActorMaterializer()

    Source(1 to 10).reduce((t1, t2) => t1 + t2).runWith(Sink.foreach(println))
    // 55
  }

  def interspaceProcessing(): Unit = {
    implicit val actor = ActorSystem("my-actor")
    implicit val mater = ActorMaterializer()

    val source = Source(List("1", "2", "3")).intersperse("-") ++ Source.single("END")
    source.runWith(Sink.foreach(println))
    // 1, -, 2, -, 3, END

  }

  def interspaceProcessing2(): Unit = {
    implicit val actor = ActorSystem("my-actor")
    implicit val mater = ActorMaterializer()

    val source = Source(List("1", "2", "3")).intersperse("[", "-", "]")
    source.runWith(Sink.foreach(println))
    // [, 1, -, 2, -, 3, ]
  }

  def groupWithin(): Unit = {
    implicit val actor = ActorSystem("my-actor")
    implicit val mater = ActorMaterializer()


  }


}
