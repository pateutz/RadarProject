package pubgradar

import pubgradar.ui.GLMap
import java.util.Collections.newSetFromMap
import java.util.concurrent.ConcurrentHashMap

const val gridWidth = 813000f
const val mapWidth = 819200f

var gameStarted = false
var isErangel = true

interface GameListener {
  fun onGameOver()
}

private val gameListeners = newSetFromMap(ConcurrentHashMap<GameListener,Boolean>())

fun register(gameListener:GameListener) {
  gameListeners.add(gameListener)
}

fun deregister(gameListener:GameListener) {
  gameListeners.remove(gameListener)
}

fun gameStart() {
  println("New Game on")
  
  gameStarted = true
}

fun gameOver() {
  gameStarted = false
  gameListeners.forEach {it.onGameOver()}
}

lateinit var Args: Array<String>
fun main(args: Array<String>) {
  Args = args
  when {
    args.size < 3 -> {
      println("usage: <ip> <sniff option> <gaming pc>")
      System.exit(-1)

    }
    args.size > 3 -> {

      println("Loading PCAP File.")

      Sniffer.sniffLocationOffline()
      val ui = GLMap()
      ui.show()
    }
    else -> {
      Sniffer.sniffLocationOnline()
      val ui = GLMap()
      ui.show()
    }
  }
}