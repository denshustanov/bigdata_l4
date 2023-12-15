package twofc.client

import org.apache.zookeeper.{CreateMode, WatchedEvent, Watcher, ZooDefs, ZooKeeper}

import scala.collection.convert.ImplicitConversions.`collection AsScalaIterable`
import scala.util.Random
import scala.util.control.Breaks.{break, breakable}

case class Client(hostPort: String, root: String, id: Int) extends Watcher{
  val zk = new ZooKeeper(hostPort, 3000, this)
  val mutex = new Object()
  var transactionId = 0
  var status = ""

  def start(): Unit = {
    var done = false
    mutex.synchronized {
      while (!done) {
        breakable {
          if (transactionId == 0) {
            val transactions = zk.getChildren(root, this).filter(e => e.startsWith("transaction_"))
            if (transactions.isEmpty) {
              println("waiting for transactions")
              mutex.wait()
              break()
            }
            transactionId = transactions.map(e => e.stripPrefix("transaction_").toInt).max
          }
        }
        status = new String(zk.getData(s"$root/transaction_$transactionId", this, null))
        println(s"current transaction: $transactionId, status: $status")
        if (status == "commit" || status == "abort") {
          println(s"status decided: $status")
          zk.setData(s"$root/transaction_$transactionId/client_$id",
            status.getBytes(),
            -1)
          done = true
        } else{
          val children = zk.getChildren(s"$root/transaction_$transactionId", this)
          println(s"current votes: $children")
          if(!children.contains(s"client_$id")) {
            println(s"new transaction!")
            val result = if (Random.nextBoolean()) "commit" else "abort"
            println(s"voting for $result")
            zk.create(s"$root/transaction_$transactionId/client_$id",
              result.getBytes(),
              ZooDefs.Ids.OPEN_ACL_UNSAFE,
              CreateMode.PERSISTENT)
          }
          mutex.wait()
        }
      }
    }
  }

  override def process(event: WatchedEvent): Unit = {
    mutex.synchronized{
      mutex.notifyAll()
    }
  }
}
