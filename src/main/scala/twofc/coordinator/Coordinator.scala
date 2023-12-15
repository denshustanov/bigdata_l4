package twofc.coordinator

import org.apache.zookeeper.{CreateMode, WatchedEvent, Watcher, ZooDefs, ZooKeeper}

import scala.collection.convert.ImplicitConversions.`collection AsScalaIterable`
import scala.collection.mutable

case class Coordinator(hostPort: String, root: String, clientsCount: Int) extends Watcher {
  val zk = new ZooKeeper(hostPort, 3000, this)
  val mutex = new Object()
  var currentCommit = ""
  type VotesMap = mutable.Map[String, String]
  var votes: VotesMap = mutable.Map()

  def start(): Unit = {


    val latestCommit = zk.getChildren(s"$root", this)
      .filter(p => p.startsWith("commit_"))
      .map(e => e.stripPrefix("commit_").toInt).max

    currentCommit = s"commit_${latestCommit + 1}"
    println(s"starting $currentCommit")


    zk.create(s"$root/$currentCommit",
      Array.emptyByteArray,
      ZooDefs.Ids.OPEN_ACL_UNSAFE,
      CreateMode.PERSISTENT)

    mutex.synchronized {

      while (votes.size < clientsCount) {
        val children = zk.getChildren(s"$root/$currentCommit", this)
        val clients = children.filter(e => e.startsWith("client_"))
        var newVote = false
        for (client <- clients) {
          if (!votes.keys.exists(_ == client)) {
            newVote = true
            val data = new String(zk.getData(s"$root/$currentCommit/$client", this, null))
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
    val commits = votes.count(e => e._2 == "commit")
    val result = if (commits > aborts) "commit" else "abort"
    println(s"performing $result")
    zk.setData(s"$root/$currentCommit", result.getBytes, -1)
  }

  override def process(event: WatchedEvent): Unit = {
    println(s"Event from zookeeper: ${event.getType}")
    mutex.synchronized {
      mutex.notifyAll()
    }
  }
}
