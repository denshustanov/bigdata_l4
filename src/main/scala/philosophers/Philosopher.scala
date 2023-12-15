package philosophers

import org.apache.log4j.Logger
import org.apache.zookeeper.{CreateMode, WatchedEvent, Watcher, ZooDefs, ZooKeeper}

import scala.util.Random

case class Philosopher(hostPort: String, root: String, id: Int, partySize: Int, lifeTime: Int) extends Watcher{
  private val leftForkNode = s"$id"
  private val rightForkNode = s"${(id+1) % partySize}"
  private val leftForkPath = s"$root/$leftForkNode"
  val rightForkPath = s"$root/$rightForkNode"
  val lockPath = s"$root/lock"
  println(s"forks: $leftForkPath, $rightForkPath")

  val log = Logger.getLogger(id.toString)

  val zk = new ZooKeeper(hostPort, 3000, this)
  val mutex = new Object()

  def start(): Unit = {
    mutex.synchronized {
      val startTime = System.currentTimeMillis();
      var delay = 0
      while (System.currentTimeMillis() < startTime + lifeTime) {
        if (checkForks()) {
          log.info(s"forks available, taking fork_${leftForkNode} and fork_${rightForkNode}")
          zk.create(leftForkPath, Array.emptyByteArray, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL)
          zk.create(rightForkPath, Array.emptyByteArray, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL)
          delay = Random.nextInt(5000)
          log.info(s"eating for $delay...")
          Thread.sleep(delay)
          zk.delete(leftForkPath, 0)
          zk.delete(rightForkPath, 0)
          delay = Random.nextInt(5000)
          log.info(s"thinking for $delay...")
          Thread.sleep(delay)
        } else {
          log.info(s"waiting for fork_${leftForkNode} and fork_${rightForkNode}")
          mutex.wait()
        }
      }
    }
  }

  override def process(event: WatchedEvent): Unit = {
    mutex.synchronized {
      println(s"Event from zookeeper: ${event.getType}")
      mutex.notifyAll()
    }
  }

  private def checkForks(): Boolean = {
    val children = zk.getChildren(root, this)
    if(children.contains(leftForkNode) || children.contains(rightForkNode)){
      return false;
    }
    true;
  }
}
