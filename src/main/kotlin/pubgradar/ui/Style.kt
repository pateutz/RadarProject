package pubgradar.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Color.YELLOW
import pubgradar.*
import pubgradar.util.fromHsv

const val initialWindowWidth=1000f
const val windowToMapUnit=mapWidth/initialWindowWidth

const val runSpeed=6.3*100//6.3m/s
const val unit=gridWidth/8
const val unit2=unit/10

//1m=100

const val miniMapWindowWidth=400f
const val miniMapRadius=500*100f
const val playerRadius=4000f
const val healthBarWidth=15000f
const val healthBarHeight=2000f
const val directionRadius=16000f
const val visionRadius=mapWidth/8
const val fov=90f

const val aimLineWidth=1000f
const val aimLineRange=50000f
const val aimCircleRadius=200f
const val aimTimeThreshold=1000
const val attackLineDuration=1000
const val attackMeLineDuration=10000
const val firingLineDuration = 500
const val firingLineLength = 20000f
const val itemZoomThreshold=0.06f

const val itemScale = 12f
const val staticItemScale = 200f
const val mapMarkerScale = 150f
const val airDropScale = 150f
const val vehicleScale = 25f
const val planeScale = 200f
const val grenadeScale = 15f
const val corpseScale = 4f
const val redzoneBombScale = 50f
const val redzongBombShowDuration = 3000

val bgColor=Color(0.417f,0.417f,0.417f,1f)
val selfColor=Color(0x32cd32ff)
val teamColor=arrayOf(Color(1f,0.5f,0f,1f),
                      Color(1f,1f,0f,1f),
                      Color(0f,0.58f,1f,1f),
                      Color(0.714f,1f,0f,1f))
val safeDirectionColor=Color(1f,1f,1f,0.5f)
val visionColor=Color(1f,1f,1f,0.1f)
val parachuteColor=Color(0.94f,1.0f,1.0f,1f)
val playerColor=Color.RED!!

val sightColor=Color(1f,1f,1f,0.5f)

val aimLineColor=Color(0f,0f,1f,1f)
val firingLineColor = Color(1.0f,1.0f,1.0f,0.5f)
val attackLineColor=Color(1.0f,0f,0f,1f)
val redZoneColor=Color(1f,0f,0f,0.2f)
val safeZoneColor=Color(1f,1f,1f,0.5f)
val airDropLineColor = YELLOW

val teamNumberColors=HashMap<Int,String>().apply {
  val num=100
  val unit=360f/num
  for (i in 0 until num) {
    put(i,fromHsv(i*unit,1f,1f).toString())
  }
}