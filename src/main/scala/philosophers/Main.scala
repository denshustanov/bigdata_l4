package philosophers

import org.apache.log4j.BasicConfigurator

import scala.collection.immutable.Seq

object Main {
  def main(args: Array[String]): Unit = {
    BasicConfigurator.configure()
//    println(args.toSeq)
//    val Seq(hostPort, partySize, id) = args.toSeq
    val philosopher = Philosopher(hostPort = args(0), root = "/ph", id = args(2).toInt, partySize = args(1).toInt, lifeTime = 100000)
    try {
      philosopher.start()
    } catch {
      case e: Exception => println("Exception in Philosopher: " + e)
    }
  }
}
