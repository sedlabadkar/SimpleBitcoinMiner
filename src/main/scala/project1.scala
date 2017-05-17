import akka.actor._
import akka.routing.RoundRobinPool
import scala.util.Random
import scala.collection.mutable.ArrayBuffer
import java.lang.Object
import java.lang.String
import java.security.MessageDigest
import java.io.File
import com.typesafe.config.ConfigFactory


case class initiateMining (numZeros:Integer, connectedAsRemote:Boolean)

case class startBitCoinMining(numZeros:Integer, gatorID:String, numAttempts:Integer)

case class bitcoinsFound(bitcoinStr:ArrayBuffer[String])

case class connectToMaster(ipAddress:String)

 //Worker mines the bitcoins and reports it back to its master. A worker's master is always local.
class Worker extends Actor {
  def receive = {    
  case startBitCoinMining(numZeros:Integer, gatorID:String, numAttempts:Integer) => {
     var x = numAttempts
     var arrBitcoinsMined:ArrayBuffer[String]=  ArrayBuffer[String]()
     //println ("StartBitcoinmining: zeros " + numZeros + " gatorid: " + gatorID)
     while (x > 0)
     {
       
       var randomStr1:String = Random.alphanumeric.take(6).mkString 
       var randomStr2:String = Random.alphanumeric.take(6).mkString
       var finalString:String = gatorID + randomStr1 + randomStr2
       val sha256hash = MessageDigest.getInstance("SHA-256").digest(finalString.getBytes)
       val sha256hashStr:String = (sha256hash.map("%02X" format _)).mkString
       val leadingZeros:String = "0" * numZeros
       
       //we have the SHA-256 hash string. Now find it this is the desired SHA-256 hash
       if (sha256hashStr.startsWith(leadingZeros))
         arrBitcoinsMined += finalString + "  " + sha256hashStr     
       x = x - 1
     }
      //println ("Total Bitcoins mined:  " + arrBitcoinsMined.length)
      //arrBitcoinsMined.foreach(println)
      sender ! bitcoinsFound(arrBitcoinsMined)
     
    }
  } 
}

//Master assigns work, consolidate results and reports findings
class Master extends Actor {
  var returnCount:Integer = 0
  var numLeadingZeros:Integer = 0
  var count:Integer = 0
  var numRemoteActors:Integer = 0
  var arrMinedCoins:ArrayBuffer[String]=  ArrayBuffer[String]()
  var remoteActors = new Array[ActorRef](10)
  val processorCount = Runtime.getRuntime().availableProcessors()
  var remoteMasterRef:ActorRef = self
  def receive = {
    case initiateMining(numZeros:Integer, connectedAsRemote:Boolean) => {
      val gatorID = "sachinedlabadkar"
      var actorCount = 3 * processorCount 
      numLeadingZeros = numZeros
      if (connectedAsRemote) //We have a remote master to report to
      {
        
        println ("\nConnected. Mining Bitcoins...")
        remoteMasterRef = sender
      }
      else // No remote master. Work locally and wait for others to connect
        println ("\nMining Bitcoins...")
      //Initialize Local workers
      val worker = context.actorOf(Props[Worker].withRouter(RoundRobinPool(actorCount)))
      for ( n <- 0 to actorCount-1)
      {
        worker ! startBitCoinMining(numZeros, gatorID, 100000)
        count = count + 1
      }
      //println (actorCount + " count: " + count)
    }
    case bitcoinsFound(bitcoinStr:ArrayBuffer[String]) => {
      arrMinedCoins.appendAll(bitcoinStr)  //consolidate all mined coins into a single place
      returnCount = returnCount + 1
      if (returnCount == count + numRemoteActors)  //All workers have finished mining. 
      {
        if (remoteMasterRef == self)  //Print mined coins when we are the master and have no other remote master to report to
        {
          arrMinedCoins.foreach(println)
          println ("\nBitcoins mining finished. Total bitcoins mined: " + arrMinedCoins.length)
          var j = 0
           while (remoteActors(j) != null)  //Shutdown all remote systems
            {
              remoteActors(j) ! "shutdown"
              j = j + 1
            }
            context.system.shutdown() 
        }
        else  //When we have a remote master to report to
        {
          remoteMasterRef ! bitcoinsFound (arrMinedCoins)
          println ("\nBitcoins mining finished. Total bitcoins mined: " + arrMinedCoins.length)
        }
      }
    }
    case connectToMaster (ipAddress:String) => {
      println ("\nConnecting to " + ipAddress + "...")
      val remoteMaster=context.actorSelection("akka.tcp://masterSystem@"+ipAddress+":5100/user/TheBoss")
      remoteMaster ! "ready"
    }
    case "ready" => {
      println ("\nRemote System connected")
      sender ! initiateMining (numLeadingZeros, true)
      remoteActors(numRemoteActors) = sender 
      numRemoteActors = numRemoteActors + 1
    }
    case "shutdown" => {
      println ("\nBye")
      context.system.shutdown()
    }
  }
}

object project1 extends App {

  if (args.length < 1) //No arguments entered
  {
    println ("\nToo Few Arguments. Usage: scala project1 <numZeros/IP address>")
    System.exit(0)
  }
  
  var cmdArgs:String=args(0)
  if (cmdArgs.contains('.')) //ipaddress entered
  {
      val ipAddress = args(0)
      //println("Input is an IP address. We are not the master." + ipAddress)
      val configFile1 = getClass.getClassLoader.getResource("local_application.conf").getFile
      val config1 = ConfigFactory.parseFile(new File(configFile1))
      val workerActorSystem = ActorSystem ("workerSystem", config1)
      val localMaster = workerActorSystem.actorOf(Props[Master], name = "LocalManager")
         localMaster ! connectToMaster (ipAddress)
  }
  else //Number of zeros entered
  {
      val configFile = getClass.getClassLoader.getResource("server_application.conf").getFile
      val config = ConfigFactory.parseFile(new File(configFile))
      val numZeros = args(0).toInt
      val masterActorSystem = ActorSystem("masterSystem", config)
      val masterActor = masterActorSystem.actorOf(Props[Master], name = "TheBoss")
    
      masterActor ! initiateMining(numZeros, false)
  }
}
