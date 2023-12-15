package zoo

import org.apache.log4j.BasicConfigurator

import scala.util.Random

object Main {
  val sleepTime = 100

  def main(args: Array[String]): Unit = {
    BasicConfigurator.configure()
    println("Starting anumal runner")
    val Seq(animalName, hostPort, partySize) = args.toSeq
    val animal = Animal(animalName, hostPort, "/zoo", partySize.toInt)

    try{
      animal.enter()
      println(s"${animal.animalName} entered.")
      for (i <- 1 to Random.nextInt(100)) {
        Thread.sleep(sleepTime)
        println(s"${animal.animalName} is running...")
      }

      animal.leave()
    } catch {
      case e: Exception => println("Animal was not permitted to the zoo. " + e)
    }

  }

}
