package twofc.client

object Main {
 def main(args: Array[String]): Unit = {
   val client = Client(args(0), "/2fc", args(1).toInt)
   client.start()
 }
}
