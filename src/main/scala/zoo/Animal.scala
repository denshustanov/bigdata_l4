package zoo

import org.apache.zookeeper.{CreateMode, WatchedEvent, Watcher, ZooDefs, ZooKeeper}

case class Animal(animalName: String, hostPort: String, root: String, partySize: Integer) extends Watcher{

  private val zk = new ZooKeeper(hostPort, 3000, this)
  private val mutex = new Object()
  private val animalPath = s"$root/$animalName"

  if (zk==null) throw new Exception("ZK is null.");
  def enter(): Boolean = {
    zk.create(animalPath, Array.emptyByteArray, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL)
    mutex.synchronized {
      while (true){
        val party = zk.getChildren(root, this)
        println(party.size())
        if(party.size() < partySize){
          println("Waiting for the others.")
          mutex.wait()
          println("Noticed someone")
        } else{
          return true
        }
      }
      return false
    }
  }

  def leave(): Unit = {
    zk.close()
  }

  override def process(event: WatchedEvent): Unit = {
    mutex.synchronized {
      println(s"Event from zookeeper: ${event.getType}")
      mutex.notifyAll()
    }
  }
}
