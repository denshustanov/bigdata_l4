package twofc.coordinator

import org.apache.zookeeper.{CreateMode, WatchedEvent, Watcher, ZooDefs, ZooKeeper}

import scala.collection.convert.ImplicitConversions.`collection AsScalaIterable`
import scala.collection.mutable

case class Coordinator(hostPort: String, root: String, clientsCount: Int) extends Watcher {
  val zk = new ZooKeeper(hostPort, 3000, this)
  val mutex = new Object()
  var currentTransaction = ""
  type VotesMap = mutable.Map[String, String]
  var votes: VotesMap = mutable.Map()

  def start(): Unit = {
    val commits = zk.getChildren(s"$root", this)
      .filter(p => p.startsWith("transaction_"))
    println(commits)

    val latestCommit =if(commits.nonEmpty) commits
      .map(e => e.stripPrefix("transaction_").toInt).max else 1

    currentTransaction = s"transaction_${latestCommit + 1}"
    println(s"starting $currentTransaction")


    zk.create(s"$root/$currentTransaction",
      "preparing".getBytes,
      ZooDefs.Ids.OPEN_ACL_UNSAFE,
      CreateMode.PERSISTENT)

    mutex.synchronized {

      while (votes.size < clientsCount) {
        val children = zk.getChildren(s"$root/$currentTransaction", this)
        val clients = children.filter(e => e.startsWith("client_"))
        var newVote = false
        for (client <- clients) {
          if (!votes.keys.exists(_ == client)) {
            newVote = true
            val data = new String(zk.getData(s"$root/$currentTransaction/$client", this, null))
            votes.put(client, data)
            println(s"new vote! $client : $data")
          }
        }
        if (votes.size < clientsCount) {
          println("waiting for votes...")
          mutex.wait()
        }
      }
    }

    println(s"got all votes: $votes")

    val aborts = votes.count(e => e._2 == "abort")
    val result = if (aborts > 0) "abort" else "commit"
    println(s"performing $result")
    zk.setData(s"$root/$currentTransaction", result.getBytes, -1)
    zk.close()
  }

  override def process(event: WatchedEvent): Unit = {
    println(s"Event from zookeeper: ${event.getType}")
    mutex.synchronized {
      mutex.notifyAll()
    }
  }
}
