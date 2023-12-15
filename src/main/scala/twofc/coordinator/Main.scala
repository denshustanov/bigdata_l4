package twofc.coordinator

object Main {
  def main(args: Array[String]): Unit = {
    val coordinator = Coordinator("127.0.0.1:2181", "/2fc", 3)
    coordinator.start()
  }
}
