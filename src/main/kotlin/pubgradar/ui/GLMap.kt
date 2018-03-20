package pubgradar.ui

import com.badlogic.gdx.*
import com.badlogic.gdx.Input.Buttons.MIDDLE
import com.badlogic.gdx.Input.Buttons.LEFT
import com.badlogic.gdx.Input.Buttons.RIGHT
import com.badlogic.gdx.Input.Keys.*
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.backends.lwjgl3.*
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.Color.*
import com.badlogic.gdx.graphics.GL20.GL_TEXTURE_2D
import com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888
import com.badlogic.gdx.graphics.Texture.TextureFilter.*
import com.badlogic.gdx.graphics.g2d.*
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.*
import com.badlogic.gdx.graphics.glutils.*
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.*
import com.badlogic.gdx.math.*
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL13.GL_CLAMP_TO_BORDER
import pubgradar.*
import pubgradar.Sniffer.Companion.localAddr
import pubgradar.Sniffer.Companion.sniffOption
import pubgradar.deserializer.channel.ActorChannel.Companion.actorHasWeapons
import pubgradar.deserializer.channel.ActorChannel.Companion.actors
import pubgradar.deserializer.channel.ActorChannel.Companion.airDropLocation
import pubgradar.deserializer.channel.ActorChannel.Companion.attacks
import pubgradar.deserializer.channel.ActorChannel.Companion.corpseLocation
import pubgradar.deserializer.channel.ActorChannel.Companion.droppedItemLocation
import pubgradar.deserializer.channel.ActorChannel.Companion.firing
import pubgradar.deserializer.channel.ActorChannel.Companion.playerStateToActor
import pubgradar.deserializer.channel.ActorChannel.Companion.redZoneBombLocation
import pubgradar.deserializer.channel.ActorChannel.Companion.selfID
import pubgradar.deserializer.channel.ActorChannel.Companion.selfStateID
import pubgradar.deserializer.channel.ActorChannel.Companion.teams
import pubgradar.deserializer.channel.ActorChannel.Companion.visualActors
import pubgradar.deserializer.channel.ActorChannel.Companion.weapons
import pubgradar.struct.*
import pubgradar.struct.Archetype.*
import pubgradar.struct.Archetype.Plane
import pubgradar.struct.Item.Companion.order
import pubgradar.struct.PlayerState
import pubgradar.struct.Team
import pubgradar.struct.Weapon
import pubgradar.struct.CMD.*
import pubgradar.struct.CMD.ActorCMD.actorWithPlayerState
import pubgradar.struct.CMD.GameStateCMD.ElapsedWarningDuration
import pubgradar.struct.CMD.GameStateCMD.MatchElapsedMinutes
import pubgradar.struct.CMD.GameStateCMD.NumAlivePlayers
import pubgradar.struct.CMD.GameStateCMD.NumAliveTeams
import pubgradar.struct.CMD.playerNumKills
import pubgradar.struct.CMD.GameStateCMD.PoisonGasWarningPosition
import pubgradar.struct.CMD.GameStateCMD.PoisonGasWarningRadius
import pubgradar.struct.CMD.GameStateCMD.RedZonePosition
import pubgradar.struct.CMD.GameStateCMD.RedZoneRadius
import pubgradar.struct.CMD.GameStateCMD.RemainingTime
import pubgradar.struct.CMD.GameStateCMD.SafetyZonePosition
import pubgradar.struct.CMD.GameStateCMD.SafetyZoneRadius
import pubgradar.struct.CMD.GameStateCMD.TotalWarningDuration
import pubgradar.struct.CMD.GameStateCMD.isTeamMatch
import pubgradar.struct.CMD.CharacterCMD.actorHealth
import pubgradar.util.*
import java.text.DecimalFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.math.*

typealias renderInfo = tuple4<Actor, Float, Float, Float>

val itemIcons = HashMap<String, AtlasRegion>()

class GLMap : InputAdapter(), ApplicationListener, GameListener {
    companion object {
        operator fun Vector3.component1(): Float = x
        operator fun Vector3.component2(): Float = y
        operator fun Vector3.component3(): Float = z
        operator fun Vector2.component1(): Float = x
        operator fun Vector2.component2(): Float = y

    }

    init {
        register(this)
    }

    override fun onGameOver() {
        mapCamera.zoom = 1 / 4f

        aimStartTime.clear()
        attackLineStartTime.clear()
        firingStartTime.clear()
    }

    fun show() {
        val config = Lwjgl3ApplicationConfiguration()
        config.setTitle("[${localAddr.hostAddress} ${sniffOption.name}] - VMRadar v1.2.1")
        config.setWindowIcon(Files.FileType.Internal, "icon.png")
        config.useOpenGL3(true, 3, 3)
        config.setWindowedMode(initialWindowWidth.toInt(), initialWindowWidth.toInt())
        config.setResizable(true)
        config.setBackBufferConfig(8, 8, 8, 8, 32, 0, 2)
        Lwjgl3Application(this, config)
    }

    lateinit var spriteBatch: SpriteBatch
    lateinit var shapeRenderer: ShapeRenderer
    lateinit var mapErangel: Texture
    lateinit var mapMiramar: Texture
    lateinit var map: Texture
    lateinit var fbo: FrameBuffer
    lateinit var miniMap: TextureRegion
    lateinit var carePackage: TextureRegion
    lateinit var corpseIcon: TextureRegion
    lateinit var vehicleIcons: Map<Archetype, TextureRegion>
    lateinit var grenadeIcons: Map<Archetype, TextureRegion>
    lateinit var redzoneBombIcon: TextureRegion
    lateinit var largeFont: BitmapFont
    lateinit var littleFont: BitmapFont
    lateinit var fontCamera: OrthographicCamera
    lateinit var camera: OrthographicCamera
    lateinit var mapCamera: OrthographicCamera
    lateinit var miniMapCamera: OrthographicCamera
    lateinit var alarmSound: Sound
    lateinit var pawnAtlas: TextureAtlas
    lateinit var itemAtlas: TextureAtlas
    lateinit var markerAtlas: TextureAtlas
    lateinit var markers: Array<TextureRegion>
    private lateinit var parachute: Texture
    private lateinit var teamarrow: Texture
    private lateinit var teamsight: Texture
    private lateinit var arrow: Texture
    private lateinit var arrowsight: Texture
    private lateinit var jetski: Texture
    private lateinit var player: Texture
    private lateinit var playersight: Texture

    private lateinit var hubFont: BitmapFont
    private lateinit var hubFontShadow: BitmapFont
    private lateinit var espFont: BitmapFont
    private lateinit var espFontShadow: BitmapFont
    private lateinit var compaseFont: BitmapFont
    private lateinit var compaseFontShadow: BitmapFont
    private lateinit var littleFontShadow: BitmapFont
    private lateinit var nameFont: BitmapFont
    private lateinit var itemFont: BitmapFont
    private lateinit var hporange: BitmapFont
    private lateinit var hpred: BitmapFont
    private lateinit var hpgreen: BitmapFont
    private lateinit var menuFont: BitmapFont
    private lateinit var menuFontOn: BitmapFont
    private lateinit var menuFontOFF: BitmapFont
    private lateinit var hubpanel: Texture
    private lateinit var hubpanelblank: Texture
    private lateinit var menu: Texture
    private lateinit var bgcompass: Texture
    val firingStartTime = LinkedList<tuple4<Float, Float, Float, Long>>()
    private val layout = GlyphLayout()
    private var windowWidth = initialWindowWidth
    private var windowHeight = initialWindowWidth
    val clipBound = Rectangle()
    private val aimStartTime = HashMap<NetworkGUID, Long>()
    private val attackLineStartTime = LinkedList<Triple<NetworkGUID, NetworkGUID, Long>>()
    private val pinLocation = Vector2()
    // Menu Settings
    //////////////////////////////
    private var filterWeapon = -1
    private var filterAttach = -1
    private var filterLvl2 = -1
    private var filterScope = -1
    private var filterHeals = -1
    private var filterAmmo = 1
    private var filterThrow = 1
    private var drawcompass = -1
    private var drawmenu = 1
    private var toggleView = 1
    private var drawDaMap = 1
    // private var toggleVehicles = -1
    //  private var toggleVNames = -1
    private var drawMinimap = -1
    private var nameToggles = 4
    private var VehicleInfoToggles = 1
    private var ZoomToggles = 1
    ///////////////////////////
    private var scopesToFilter = arrayListOf("")
    private var weaponsToFilter = arrayListOf("")
    private var attachToFilter = arrayListOf("")
    private var level2Filter = arrayListOf("")
    private var healsToFilter = arrayListOf("")
    private var ammoToFilter = arrayListOf("")
    private var throwToFilter = arrayListOf("")
    private var dragging = false
    private var prevScreenX = -1f
    private var prevScreenY = -1f
    private var screenOffsetX = 0f
    private var screenOffsetY = 0f


    private fun windowToMap(x: Float, y: Float) =
            Vector2(selfCoords.x + (x - windowWidth / 2.0f) * mapCamera.zoom * windowToMapUnit + screenOffsetX,
                    selfCoords.y + (y - windowHeight / 2.0f) * mapCamera.zoom * windowToMapUnit + screenOffsetY)

    private fun mapToWindow(x: Float, y: Float) =
            Vector2((x - selfCoords.x - screenOffsetX) / (mapCamera.zoom * windowToMapUnit) + windowWidth / 2.0f,
                    (y - selfCoords.y - screenOffsetY) / (mapCamera.zoom * windowToMapUnit) + windowHeight / 2.0f)


    fun Vector2.windowToMap() = windowToMap(x, y)
    fun Vector2.mapToWindow() = mapToWindow(x, y)
    fun windowToMap(length: Float) = length * mapCamera.zoom * windowToMapUnit
    fun mapToWindow(length: Float) = length / (mapCamera.zoom * windowToMapUnit)


    override fun scrolled(amount: Int): Boolean {

        if (mapCamera.zoom >= 0.01f && mapCamera.zoom <= 1f) {
            mapCamera.zoom *= 1.05f.pow(amount)
            miniMapCamera.zoom = if (mapCamera.zoom > 1 / 8f) 1 / 2f else 1 / 4f
        } else {
            if (mapCamera.zoom < 0.01f) {
                mapCamera.zoom = 0.01f
                println("Max Zoom")
            }
            if (mapCamera.zoom > 1f) {
                mapCamera.zoom = 1f
                println("Min Zoom")
            }
        }

        return true
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        when (button) {
            RIGHT -> {
                pinLocation.set(pinLocation.set(screenX.toFloat(), screenY.toFloat()).windowToMap())
                camera.update()
                println(pinLocation)
                return true
            }
            LEFT -> {
                dragging = true
                prevScreenX = screenX.toFloat()
                prevScreenY = screenY.toFloat()
                return true
            }
            MIDDLE -> {
                screenOffsetX = 0f
                screenOffsetY = 0f
            }
        }
        return false
    }

    override fun keyDown(keycode: Int): Boolean {

        when (keycode) {


        // Change Player Info
            F1 -> {
                if (nameToggles < 5) {
                    nameToggles += 1
                }
                if (nameToggles == 5) {
                    nameToggles = 0
                }
            }

            F5 -> {
                if (VehicleInfoToggles <= 4) {
                    VehicleInfoToggles += 1
                }
                if (VehicleInfoToggles == 4) {
                    VehicleInfoToggles = 1
                }
            }
        // Zoom (Loot, Combat, Scout)
            NUMPAD_8 -> {
                if (ZoomToggles <= 4) {
                    ZoomToggles += 1
                }
                if (ZoomToggles == 4) {
                    ZoomToggles = 1
                }
                if (ZoomToggles == 1) {
                    mapCamera.zoom = 1 / 8f
                    camera.zoom = 1 / 24f

                }
                if (ZoomToggles == 2) {
                    mapCamera.zoom = 1 / 12f
                    camera.zoom = 1 / 12f
                }
                if (ZoomToggles == 3) {
                    mapCamera.zoom = 1 / 24f
                    camera.zoom = 1 / 8f
                }
            }
        // Other Filter Keybinds
            F2 -> drawcompass = drawcompass * -1


        // Toggle View Line
            F4 -> toggleView = toggleView * -1

        // Toggle Da Minimap
            F3 -> drawDaMap = drawDaMap * -1

        // Toggle Menu
            F12 -> drawmenu = drawmenu * -1

        // Icon Filter Keybinds
            NUMPAD_1 -> filterWeapon = filterWeapon * -1
            NUMPAD_2 -> filterLvl2 = filterLvl2 * -1
            NUMPAD_3 -> filterHeals = filterHeals * -1
            NUMPAD_4 -> filterThrow = filterThrow * -1
            NUMPAD_5 -> filterAttach = filterAttach * -1
            NUMPAD_6 -> filterScope = filterScope * -1
            NUMPAD_0 -> filterAmmo = filterAmmo * -1

        // Zoom In/Out || Overrides Max/Min Zoom
            MINUS -> camera.zoom = camera.zoom + 0.00525f
            PLUS -> camera.zoom = camera.zoom - 0.00525f

        }
        return false
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        if (!dragging) return false
        with(camera) {
            screenOffsetX += (prevScreenX - screenX.toFloat()) * camera.zoom * 500
            screenOffsetY += (prevScreenY - screenY.toFloat()) * camera.zoom * 500
            prevScreenX = screenX.toFloat()
            prevScreenY = screenY.toFloat()
        }
        return true
    }


    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        if (button == LEFT) {
            dragging = false
            return true
        }
        return false
    }


    override fun create() {
        spriteBatch = SpriteBatch()
        shapeRenderer = ShapeRenderer()
        Gdx.input.inputProcessor = this
        mapCamera = OrthographicCamera(windowWidth, windowHeight)
        miniMapCamera = OrthographicCamera()
        with(mapCamera) {
            setToOrtho(true, windowWidth * windowToMapUnit, windowHeight * windowToMapUnit)
            zoom = 1 / 4f
            update()
            position.set(mapWidth / 2, mapWidth / 2, 0f)
            update()
        }
        with(miniMapCamera) {
            val z = 1 / 4f
            setToOrtho(true, miniMapRadius * 2 / z, miniMapRadius * 2 / z)
            zoom = z
            update()
        }
        camera = mapCamera
        fontCamera = OrthographicCamera(initialWindowWidth, initialWindowWidth)
        alarmSound = Gdx.audio.newSound(Gdx.files.internal("Alarm.wav"))
        glTexParameterfv(GL_TEXTURE_2D, GL_TEXTURE_BORDER_COLOR, floatArrayOf(bgColor.r, bgColor.g, bgColor.b, bgColor.a))
        mapErangel = Texture(Gdx.files.internal("maps/Erangel_Minimap.png"), null, true).apply {
            setFilter(MipMap, Linear)
            Gdx.gl.glTexParameterf(glTarget, GL20.GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER.toFloat())
            Gdx.gl.glTexParameterf(glTarget, GL20.GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER.toFloat())
        }
        mapMiramar = Texture(Gdx.files.internal("maps/Miramar_Minimap.png"), null, true).apply {
            setFilter(MipMap, Linear)
            Gdx.gl.glTexParameterf(glTarget, GL20.GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER.toFloat())
            Gdx.gl.glTexParameterf(glTarget, GL20.GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER.toFloat())
        }
        map = mapErangel
        fbo = FrameBuffer(RGBA8888, miniMapWindowWidth.toInt(), miniMapWindowWidth.toInt(), false)
        miniMap = TextureRegion(fbo.colorBufferTexture)

        hubpanel = Texture(Gdx.files.internal("images/hub_panel.png"))
        menu = Texture(Gdx.files.internal("images/menu.png"))
        bgcompass = Texture(Gdx.files.internal("images/bg_compass.png"))
        arrow = Texture(Gdx.files.internal("images/arrow.png"))
        player = Texture(Gdx.files.internal("images/player.png"))
        playersight = Texture(Gdx.files.internal("images/green_view_line.png"))
        teamsight = Texture(Gdx.files.internal("images/teamsight.png"))
        arrowsight = Texture(Gdx.files.internal("images/red_view_line.png"))
        teamarrow = Texture(Gdx.files.internal("images/team.png"))
        parachute = Texture(Gdx.files.internal("images/parachute.png"))

        parachute = Texture(Gdx.files.internal("images/parachute.png"))
        itemAtlas = TextureAtlas(Gdx.files.internal("icons/itemIcons.txt"))
        for (region in itemAtlas.regions)
            itemIcons[region.name] = region.apply { flip(false, true) }

        pawnAtlas = TextureAtlas(Gdx.files.internal("icons/APawnIcons.txt"))
        for (region in pawnAtlas.regions)
            region.flip(false, true)

        carePackage = pawnAtlas.findRegion("CarePackage")
        corpseIcon = pawnAtlas.findRegion("corpse")
        redzoneBombIcon = pawnAtlas.findRegion("redzoneBomb")
        vehicleIcons = mapOf(
                TwoSeatBoat to pawnAtlas.findRegion("AquaRail"),
                SixSeatBoat to pawnAtlas.findRegion("boat"),
                Dacia to pawnAtlas.findRegion("dacia"),
                Uaz to pawnAtlas.findRegion("uaz"),
                Pickup to pawnAtlas.findRegion("pickup"),
                Buggy to pawnAtlas.findRegion("buggy"),
                Bike to pawnAtlas.findRegion("bike"),
                SideCar to pawnAtlas.findRegion("bike"),
                Bus to pawnAtlas.findRegion("bus"),
                Plane to pawnAtlas.findRegion("plane")
        )
        grenadeIcons = mapOf(
                SmokeBomb to pawnAtlas.findRegion("smokebomb"),
                Molotov to pawnAtlas.findRegion("molotov"),
                Grenade to pawnAtlas.findRegion("fragbomb"),
                FlashBang to pawnAtlas.findRegion("flashbang")
        )


        markerAtlas = TextureAtlas(Gdx.files.internal("icons/Markers.txt"))
               for (region in markerAtlas.regions)
                        region.flip(false,true)


        markers = arrayOf(markerAtlas.findRegion("marker1"), markerAtlas.findRegion("marker2"),
                markerAtlas.findRegion("marker3"), markerAtlas.findRegion("marker4"))

        val generatorHub = FreeTypeFontGenerator(Gdx.files.internal("font/AGENCYFB.TTF"))
        val paramHub = FreeTypeFontParameter()
        paramHub.characters = DEFAULT_CHARS
        paramHub.size = 30
        paramHub.color = WHITE
        hubFont = generatorHub.generateFont(paramHub)
        paramHub.color = Color(1f, 1f, 1f, 0.4f)
        hubFontShadow = generatorHub.generateFont(paramHub)
        paramHub.size = 16
        paramHub.color = WHITE
        espFont = generatorHub.generateFont(paramHub)
        paramHub.color = Color(1f, 1f, 1f, 0.2f)
        espFontShadow = generatorHub.generateFont(paramHub)
        val generatorNumber = FreeTypeFontGenerator(Gdx.files.internal("font/NUMBER.TTF"))
        val paramNumber = FreeTypeFontParameter()
        paramNumber.characters = DEFAULT_CHARS
        paramNumber.size = 24
        paramNumber.color = WHITE
        largeFont = generatorNumber.generateFont(paramNumber)
        val generator = FreeTypeFontGenerator(Gdx.files.internal("font/GOTHICB.TTF"))
        val param = FreeTypeFontParameter()
        param.characters = DEFAULT_CHARS
        param.size = 38
        param.color = WHITE
        largeFont = generator.generateFont(param)
        param.size = 15
        param.color = WHITE
        littleFont = generator.generateFont(param)
        param.color = BLACK
        param.size = 10
        nameFont = generator.generateFont(param)
        param.color = WHITE
        param.size = 6
        itemFont = generator.generateFont(param)
        val compaseColor = Color(0f, 0.95f, 1f, 1f)  //Turquoise1
        param.color = compaseColor
        param.size = 10
        compaseFont = generator.generateFont(param)
        param.color = Color(0f, 0f, 0f, 0.5f)
        compaseFontShadow = generator.generateFont(param)
        param.characters = DEFAULT_CHARS
        param.size = 20
        param.color = WHITE
        littleFont = generator.generateFont(param)
        param.color = Color(0f, 0f, 0f, 0.5f)
        littleFontShadow = generator.generateFont(param)
        param.color = WHITE
        param.size = 12
        menuFont = generator.generateFont(param)
        param.color = GREEN
        param.size = 12
        menuFontOn = generator.generateFont(param)
        param.color = RED
        param.size = 12
        menuFontOFF = generator.generateFont(param)
        param.color = ORANGE
        param.size = 10
        hporange = generator.generateFont(param)
        param.color = GREEN
        param.size = 10
        hpgreen = generator.generateFont(param)
        param.color = RED
        param.size = 10
        hpred = generator.generateFont(param)


        generatorHub.dispose()
        generatorNumber.dispose()
        generator.dispose()
    }

    private val dirUnitVector = Vector2(1f, 0f)
    fun drawMapMarkers() {
        paint (camera.combined) {
            for (team in teams.values) {
                if (team.showMapMarker) {
                    //println(team.mapMarkerPosition)
                    val icon = markers[team.memberNumber]
                    val (x, y) = team.mapMarkerPosition
                    draw(icon, x, y, 0f, mapMarkerScale, false)
                }
            }
        }
    }

    override fun render() {
        Gdx.gl.glClearColor(bgColor.r, bgColor.g, bgColor.b, bgColor.a)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        if (gameStarted)
            map = if (isErangel) mapErangel else mapMiramar
        else
            return
        actors[selfID]?.apply {
            actors[attachParent ?: return@apply]?.apply {
                selfCoords.set(location.x, location.y)
                selfDirection = rotation.y
            }
        }
        val (selfX, selfY) = selfCoords
        //move camera
        mapCamera.position.set(selfCoords.x + screenOffsetX, selfCoords.y + screenOffsetY, 0f)
        mapCamera.update()

        val mapRegion = Rectangle().apply {
            setPosition(windowToMap(0f, 0f))
            width = windowToMap(windowWidth)
            height = windowToMap(windowHeight)
        }
        val miniMapRegion = Rectangle().apply {
            x = selfCoords.x - miniMapRadius
            y = selfCoords.y - miniMapRadius
            width = miniMapRadius * 2
            height = miniMapRadius * 2
        }

        var parachutes: ArrayList<renderInfo>? = null
        var players: ArrayList<renderInfo>? = null
        var vehicles: ArrayList<renderInfo>? = null
        var grenades: ArrayList<renderInfo>? = null

        for ((_, actor) in visualActors) {
            val (x, y) = actor.location
            if (!mapRegion.contains(x, y) && !miniMapRegion.contains(x, y)) continue
            val visualActor = tuple4(actor, x, y, actor.rotation.y)
            val list = when (actor.type) {
                Parachute -> {
                    parachutes = parachutes ?: ArrayList()
                    parachutes
                }
                Player -> {
                    players = players ?: ArrayList()
                    players
                }
                TwoSeatBoat, SixSeatBoat, Dacia, Uaz, Pickup, Buggy,
                Bike, SideCar, Bus, Plane -> {
                    vehicles = vehicles ?: ArrayList()
                    actor as Vehicle
                    actor.apply {
                        var driver: Actor? = null
                        for (child in attachChildren) {
                            driver = actors[child] ?: continue
                            break
                        }
                        if (driver == null && driverPlayerState.isValid()) {
                            val driverID = playerStateToActor[driverPlayerState]
                            driver = if (driverID != null) actors[driverID] else null
                        }
                        if (driver == null) return@apply
                        val _players = players ?: ArrayList()
                        _players.add(visualActor.copy(_1 = driver))
                        players = _players
                    }
                    vehicles
                }
                SmokeBomb, Molotov, Grenade, FlashBang -> {
                    grenades = grenades ?: ArrayList()
                    grenades
                }
                else -> null
            }
            list?.add(visualActor)
        }
        clipBound.set(mapRegion)
        camera = mapCamera

        //draw map
        paint(camera.combined) {
            draw(map, 0f, 0f, mapWidth, mapWidth,
                    0, 0, map.width, map.height,
                    false, true)
            drawRedZoneBomb()
            drawMapMarkers()
            drawVehicles(vehicles)
            drawCorpse()
            drawAirDrop()
            drawMapMarkers()
            drawItem()
            drawGrenades(grenades)
            drawMapMarkers()

        }


        val numKills = playerNumKills[selfStateID] ?: 0
        val zero = numKills.toString()
        paint(fontCamera.combined) {
            val timeHints = if (RemainingTime > 0) "${RemainingTime}s"
            else "${MatchElapsedMinutes}min"

            // NUMBER PANEL
            val numText = "$NumAlivePlayers"
            layout.setText(hubFont, numText)
            spriteBatch.draw(hubpanel, windowWidth - 130f, windowHeight - 60f)
            hubFontShadow.draw(spriteBatch, "ALIVE", windowWidth - 85f, windowHeight - 29f)
            hubFont.draw(spriteBatch, "$NumAlivePlayers", windowWidth - 110f - layout.width / 2, windowHeight - 29f)
            val teamText = "$NumAliveTeams"


            if (isTeamMatch) {
                layout.setText(hubFont, teamText)
                spriteBatch.draw(hubpanel, windowWidth - 260f, windowHeight - 60f)
                hubFontShadow.draw(spriteBatch, "TEAM", windowWidth - 215f, windowHeight - 29f)
                hubFont.draw(spriteBatch, "$NumAliveTeams", windowWidth - 240f - layout.width / 2, windowHeight - 29f)
            }
            if (isTeamMatch) {

                layout.setText(hubFont, zero)
                spriteBatch.draw(hubpanel, windowWidth - 390f, windowHeight - 60f)
                hubFontShadow.draw(spriteBatch, "KILLS", windowWidth - 345f, windowHeight - 29f)
                hubFont.draw(spriteBatch, "$zero", windowWidth - 370f - layout.width / 2, windowHeight - 29f)
            } else {
                spriteBatch.draw(hubpanel, windowWidth - 390f + 130f, windowHeight - 60f)
                hubFontShadow.draw(spriteBatch, "KILLS", windowWidth - 345f + 128f, windowHeight - 29f)
                hubFont.draw(spriteBatch, "$zero", windowWidth - 370f + 128f - layout.width / 2, windowHeight - 29f)

            }


            // ITEM ESP FILTER PANEL
            //  spriteBatch.draw(hubpanelblank, 30f, windowHeight - 60f)

            // This is what you were trying to do
            if (filterWeapon != 1)
                espFont.draw(spriteBatch, "WEAPON", 40f, windowHeight - 25f)
            else
                espFontShadow.draw(spriteBatch, "WEAPON", 39f, windowHeight - 25f)

            if (filterAttach != 1)
                espFont.draw(spriteBatch, "ATTACH", 40f, windowHeight - 42f)
            else
                espFontShadow.draw(spriteBatch, "ATTACH", 40f, windowHeight - 42f)

            if (filterLvl2 != 1)
                espFont.draw(spriteBatch, "EQUIP", 100f, windowHeight - 25f)
            else
                espFontShadow.draw(spriteBatch, "EQUIP", 100f, windowHeight - 25f)

            if (filterScope != 1)
                espFont.draw(spriteBatch, "SCOPE", 98f, windowHeight - 42f)
            else
                espFontShadow.draw(spriteBatch, "SCOPE", 98f, windowHeight - 42f)

            if (filterHeals != 1)
                espFont.draw(spriteBatch, "MEDS", 150f, windowHeight - 25f)
            else
                espFontShadow.draw(spriteBatch, "MEDS", 150f, windowHeight - 25f)

            if (filterAmmo != 1)
                espFont.draw(spriteBatch, "AMMO", 150f, windowHeight - 42f)
            else
                espFontShadow.draw(spriteBatch, "AMMO", 150f, windowHeight - 42f)
            if (drawcompass == 1)
                espFont.draw(spriteBatch, "COMPASS", 200f, windowHeight - 42f)
            else
                espFontShadow.draw(spriteBatch, "COMPASS", 200f, windowHeight - 42f)
            if (filterThrow != 1)
                espFont.draw(spriteBatch, "THROW", 200f, windowHeight - 25f)
            else
                espFontShadow.draw(spriteBatch, "THROW", 200f, windowHeight - 25f)

            if (drawmenu == 1)
                espFont.draw(spriteBatch, "[INS] Menu ON", 270f, windowHeight - 25f)
            else
                espFontShadow.draw(spriteBatch, "[INS] Menu OFF", 270f, windowHeight - 25f)

            val num = nameToggles
            espFontShadow.draw(spriteBatch, "[F1] Player Info: $num", 270f, windowHeight - 42f)

            val znum = ZoomToggles
            espFontShadow.draw(spriteBatch, "[Num8] Zoom Toggle: $znum", 40f, windowHeight - 68f)

            val vnum = VehicleInfoToggles
            espFontShadow.draw(spriteBatch, "[F5] Vehicle Toggles: $vnum", 40f, windowHeight - 85f)


            val pinDistance = (pinLocation.cpy().sub(selfCoords.x, selfCoords.y).len() / 100).toInt()
            val (x, y) = pinLocation.mapToWindow()


            safeZoneHint()
            drawPlayerSprites(parachutes, players)



            val camnum = camera.zoom

            if (drawmenu == 1) {
                spriteBatch.draw(menu, 20f, windowHeight / 2 - 200f)

                // Filters
                if (filterWeapon != 1)
                    menuFontOn.draw(spriteBatch, "Enabled", 187f, windowHeight / 2 + 103f)
                else
                    menuFontOFF.draw(spriteBatch, "Disabled", 187f, windowHeight / 2 + 103f)

                if (filterLvl2 != 1)
                    menuFontOn.draw(spriteBatch, "Enabled", 187f, windowHeight / 2 + 85f)
                else
                    menuFontOFF.draw(spriteBatch, "Disabled", 187f, windowHeight / 2 + 85f)

                if (filterHeals != 1)
                    menuFontOn.draw(spriteBatch, "Enabled", 187f, windowHeight / 2 + 67f)
                else
                    menuFontOFF.draw(spriteBatch, "Disabled", 187f, windowHeight / 2 + 67f)

                if (filterThrow != 1)
                    menuFontOn.draw(spriteBatch, "Enabled", 187f, windowHeight / 2 + 49f)
                else
                    menuFontOFF.draw(spriteBatch, "Disabled", 187f, windowHeight / 2 + 49f)

                if (filterAttach != 1)
                    menuFontOn.draw(spriteBatch, "Enabled", 187f, windowHeight / 2 + 31f)
                else
                    menuFontOFF.draw(spriteBatch, "Disabled", 187f, windowHeight / 2 + 31f)

                if (filterScope != 1)
                    menuFontOn.draw(spriteBatch, "Enabled", 187f, windowHeight / 2 + 13f)
                else
                    menuFontOFF.draw(spriteBatch, "Disabled", 187f, windowHeight / 2 + 13f)

                if (filterAmmo != 1)
                    menuFontOn.draw(spriteBatch, "Enabled", 187f, windowHeight / 2 + -5f)
                else
                    menuFontOFF.draw(spriteBatch, "Disabled", 187f, windowHeight / 2 + -5f)

                val camvalue = camera.zoom
                when {
                    camvalue <= 0.0100f -> menuFontOFF.draw(spriteBatch, "Max Zoom", 187f, windowHeight / 2 + -27f)
                    camvalue >= 1f -> menuFontOFF.draw(spriteBatch, "Min Zoom", 187f, windowHeight / 2 + -27f)
                    camvalue == 0.2500f -> menuFont.draw(spriteBatch, "Default", 187f, windowHeight / 2 + -27f)
                    camvalue == 0.1250f -> menuFont.draw(spriteBatch, "Scouting", 187f, windowHeight / 2 + -27f)
                    camvalue >= 0.0833f -> menuFont.draw(spriteBatch, "Combat", 187f, windowHeight / 2 + -27f)
                    camvalue <= 0.0417f -> menuFont.draw(spriteBatch, "Looting", 187f, windowHeight / 2 + -27f)

                    else -> menuFont.draw(spriteBatch, ("%.4f").format(camnum), 187f, windowHeight / 2 + -27f)
                }

                // Name Toggles
                val togs = nameToggles
                if (nameToggles >= 1)

                    menuFontOn.draw(spriteBatch, "Enabled: $togs", 187f, windowHeight / 2 + -89f)
                else
                    menuFontOFF.draw(spriteBatch, "Disabled", 187f, windowHeight / 2 + -89f)


                // Compass
                if (drawcompass != 1)

                    menuFontOFF.draw(spriteBatch, "Disabled", 187f, windowHeight / 2 + -107f)
                else
                    menuFontOn.draw(spriteBatch, "Enabled", 187f, windowHeight / 2 + -107f)


                if (drawDaMap == 1)

                    menuFontOn.draw(spriteBatch, "Enabled", 187f, windowHeight / 2 + -125f)
                else
                    menuFontOFF.draw(spriteBatch, "Disabled", 187f, windowHeight / 2 + -125f)

                if (toggleView == 1)
                    menuFontOn.draw(spriteBatch, "Enabled", 187f, windowHeight / 2 + -143f)
                else
                    menuFontOFF.draw(spriteBatch, "Disabled", 187f, windowHeight / 2 + -143f)

                if (VehicleInfoToggles < 3)
                    menuFontOn.draw(spriteBatch, "Enabled", 187f, windowHeight / 2 + -161f)
                if (VehicleInfoToggles == 3)
                    menuFontOFF.draw(spriteBatch, "Disabled", 187f, windowHeight / 2 + -161f)

                // DrawMenu == 1 already
                menuFontOn.draw(spriteBatch, "Enabled", 187f, windowHeight / 2 + -179f)
            }
            // DrawMenu == 0 (Disabled)


            if (drawcompass == 1) {

                spriteBatch.draw(bgcompass, windowWidth / 2 - 168f, windowHeight / 2 - 168f)

                layout.setText(compaseFont, "0")
                compaseFont.draw(spriteBatch, "0", windowWidth / 2 - layout.width / 2, windowHeight / 2 + layout.height + 150)                  // N
                layout.setText(compaseFont, "45")
                compaseFont.draw(spriteBatch, "45", windowWidth / 2 - layout.width / 2 + 104, windowHeight / 2 + layout.height / 2 + 104)          // NE
                layout.setText(compaseFont, "90")
                compaseFont.draw(spriteBatch, "90", windowWidth / 2 - layout.width / 2 + 147, windowHeight / 2 + layout.height / 2)                // E
                layout.setText(compaseFont, "135")
                compaseFont.draw(spriteBatch, "135", windowWidth / 2 - layout.width / 2 + 106, windowHeight / 2 + layout.height / 2 - 106)          // SE
                layout.setText(compaseFont, "180")
                compaseFont.draw(spriteBatch, "180", windowWidth / 2 - layout.width / 2, windowHeight / 2 + layout.height / 2 - 151)                // S
                layout.setText(compaseFont, "225")
                compaseFont.draw(spriteBatch, "225", windowWidth / 2 - layout.width / 2 - 109, windowHeight / 2 + layout.height / 2 - 109)          // SW
                layout.setText(compaseFont, "270")
                compaseFont.draw(spriteBatch, "270", windowWidth / 2 - layout.width / 2 - 153, windowHeight / 2 + layout.height / 2)                // W
                layout.setText(compaseFont, "315")
                compaseFont.draw(spriteBatch, "315", windowWidth / 2 - layout.width / 2 - 106, windowHeight / 2 + layout.height / 2 + 106)          // NW
            }
            littleFont.draw(spriteBatch, "$pinDistance", x, windowHeight - y)

            safeZoneHint()
            drawPlayerInfos(players)
        }


        Gdx.gl.glEnable(GL20.GL_BLEND)
        shapeRenderer.projectionMatrix = camera.combined
        draw(Line) {
            players?.forEach {
                aimAtMe(it)
            }
            drawCircles()
            drawAttackLine()
            drawAirDropLine()

        }

        draw(Filled) {
            color = redZoneColor
            circle(RedZonePosition, RedZoneRadius, 100)

            color = visionColor
            circle(selfX, selfY, visionRadius, 100)

               drawPlayersH(players)

        }
        Gdx.gl.glDisable(GL20.GL_BLEND)
        clipBound.set(miniMapRegion)
        camera = miniMapCamera

        if (drawDaMap == 1) {
            drawMiniMap(parachutes, players, vehicles)
        }}

  private fun ShapeRenderer.drawPlayersH(players:ArrayList<renderInfo>?) {
      //draw self
      drawAllPlayerHealth(selfColor,tuple4(actors[selfID] ?: return,selfCoords.x,selfCoords.y,selfDirection))
    players?.forEach {
        drawAllPlayerHealth(playerColor,it)

    }
  }

    private fun ShapeRenderer.drawPlayersMini(parachutes:ArrayList<renderInfo>?,players:ArrayList<renderInfo>?) {
        parachutes?.forEach {
            drawPlayer(parachuteColor,it)
        }
        //draw self
        drawPlayer(selfColor,tuple4(actors[selfID] ?: return,selfCoords.x,selfCoords.y,selfDirection))
        players?.forEach {
            drawPlayer(playerColor,it)
        }
    }


    private fun drawPlayerSprites(parachutes: ArrayList<renderInfo>?, players: ArrayList<renderInfo>?) {
        parachutes?.forEach {
            val (_, x, y, dir) = it
            val (sx, sy) = Vector2(x, y).mapToWindow()
            spriteBatch.draw(
                    parachute,
                    sx + 2, windowHeight - sy - 2, 4.toFloat() / 2,
                    4.toFloat() / 2, 4.toFloat(), 4.toFloat(), 8f, 8f,
                    dir * -1, 0, 0, 128, 128, true, false)
        }
        //draw self
          drawMyself(tuple4(actors[selfID] ?: return,selfCoords.x,selfCoords.y,selfDirection))
        players?.forEach {

            val (actor, x, y, dir) = it
            val (sx, sy) = Vector2(x, y).mapToWindow()
            val teamId = isTeamMate(actor)

            if (teamId > 0) {

                // Can't wait for the "Omg Players don't draw issues
                spriteBatch.draw(
                        teamarrow,
                        sx, windowHeight - sy - 2, 4.toFloat() / 2,
                        4.toFloat() / 2, 4.toFloat(), 4.toFloat(), 5f, 5f,
                        dir * -1, 0, 0, 64, 64, true, false)

                if (toggleView == 1) {
                    spriteBatch.draw(
                            teamsight,
                            sx + 1, windowHeight - sy - 2,
                            2.toFloat() / 2,
                            2.toFloat() / 2,
                            12.toFloat(), 2.toFloat(),
                            10f, 10f,
                            dir * -1, 0, 0, 512, 64, true, false)
                }

            } else {

                spriteBatch.draw(
                        arrow,
                        sx, windowHeight - sy - 2, 4.toFloat() / 2,
                        4.toFloat() / 2, 4.toFloat(), 4.toFloat(), 5f, 5f,
                        dir * -1, 0, 0, 64, 64, true, false)

                if (toggleView == 1) {
                    spriteBatch.draw(
                            arrowsight,
                            sx + 1, windowHeight - sy - 2,
                            2.toFloat() / 2,
                            2.toFloat() / 2,
                            12.toFloat(), 2.toFloat(),
                            10f, 10f,
                            dir * -1, 0, 0, 512, 64, true, false)
                }
            }

        }
    }

    private fun drawMyself(actorInfo: renderInfo) {
        val (actor, x, y, dir) = actorInfo
        val (sx, sy) = Vector2(x, y).mapToWindow()
        if (toggleView == 1) {
            // Just draw them both at the same time to avoid player not drawing ¯\_(ツ)_/¯
            spriteBatch.draw(
                    player,
                    sx, windowHeight - sy - 2, 4.toFloat() / 2,
                    4.toFloat() / 2, 4.toFloat(), 4.toFloat(), 5f, 5f,
                    dir * -1, 0, 0, 64, 64, true, false)

            spriteBatch.draw(
                    playersight,
                    sx + 1, windowHeight - sy - 2,
                    2.toFloat() / 2,
                    2.toFloat() / 2,
                    12.toFloat(), 2.toFloat(),
                    10f, 10f,
                    dir * -1, 0, 0, 512, 64, true, false)
        } else {

            spriteBatch.draw(
                    player,
                    sx, windowHeight - sy - 2, 4.toFloat() / 2,
                    4.toFloat() / 2, 4.toFloat(), 4.toFloat(), 5f, 5f,
                    dir * -1, 0, 0, 64, 64, true, false)
        }
    }

    private fun SpriteBatch.drawMapMarkers() {
        for (team in teams.values) {
            if (team.showMapMarker) {
                val icon = markers[team.memberNumber]
                val (x, y) = team.mapMarkerPosition
                draw(icon, x, y, 0f, mapMarkerScale, false)
            }
        }
    }


    fun ShapeRenderer.drawPlayer(pColor:Color?,actorInfo:renderInfo) {
        val (actor,x,y,dir) = actorInfo
        if (!clipBound.contains(x,y)) return
        val zoom = camera.zoom
        val backgroundRadius = (playerRadius+2000f)*zoom
        val playerRadius = playerRadius*zoom
        val directionRadius = directionRadius*zoom

        color = BLACK
        circle(x,y,backgroundRadius,10)

        val attach = actor.attachChildren.firstOrNull()
        val teamId = isTeamMate(actor)
        color = when {
            teamId >= 0 -> teamColor[teamId]
            attach == null -> pColor
            attach == selfID -> selfColor
            else -> {
                val teamId = isTeamMate(actors[attach])
                if (teamId >= 0)
                    teamColor[teamId]
                else
                    pColor
            }
        }
        if (actor is Character)
            color = when {
                actor.isGroggying -> {
                    GRAY
                }
                actor.isReviving -> {
                    WHITE
                }
                else -> color
            }
        circle(x,y,playerRadius,10)

        color = sightColor
        arc(x,y,directionRadius,dir-fov/2,fov,10)

        if (actor is Character) {//draw health
            val health = if (actor.health <= 0f) actor.groggyHealth else actor.health
            val width = healthBarWidth*zoom
            val height = healthBarHeight*zoom
            val y = y+backgroundRadius+height/2
            val healthWidth = (health/100.0*width).toFloat()
            color = when {
                health > 80f -> GREEN
                health > 33f -> ORANGE
                else -> RED
            }
            rectLine(x-width/2,y,x-width/2+healthWidth,y,height)
        }
    }



    private fun drawMiniMap(parachutes: ArrayList<renderInfo>?, players: ArrayList<renderInfo>?, vehicles: ArrayList<renderInfo>?) {
        fbo.begin()
        Gdx.gl.glClearColor(bgColor.r, bgColor.g, bgColor.b, bgColor.a)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        val (selfX, selfY) = selfCoords
        miniMapCamera.apply {
            position.set(selfX, selfY, 0f)
            update()
        }
        spriteBatch.projectionMatrix = miniMapCamera.combined
        paint {
            draw(map, 0f, 0f, mapWidth, mapWidth,
                    0, 0, map.width, map.height,
                    false, true)
            drawVehicles(vehicles)

        }
        shapeRenderer.projectionMatrix = miniMapCamera.combined
        Gdx.gl.glEnable(GL20.GL_BLEND)
        draw(Filled) {
          drawPlayersH(players)
            drawPlayersMini(parachutes,players)
        }
        draw(Line) {
            players?.forEach {
                aimAtMe(it)
            }
            drawCircles()
            drawAttackLine()
            drawAirDropLine()
        }
        Gdx.gl.glDisable(GL20.GL_BLEND)
        fbo.end()

        val miniMapWidth = windowToMap(miniMapWindowWidth)
        val (rx, ry) = windowToMap(windowWidth, windowHeight).sub(miniMapWidth, miniMapWidth)
        spriteBatch.projectionMatrix = mapCamera.combined
        paint {

            draw(miniMap, rx, ry, miniMapWidth, miniMapWidth)
        }
        shapeRenderer.projectionMatrix = mapCamera.combined
        Gdx.gl.glLineWidth(2f)
        draw(Line) {
            color = BLACK
            rect(rx, ry, miniMapWidth, miniMapWidth)
        }
        Gdx.gl.glLineWidth(1f)
    }

    private fun SpriteBatch.drawVehicles(vehicles: ArrayList<renderInfo>?) {
        vehicles?.forEach { (actor, x, y, dir) ->
            if (!clipBound.contains(x, y)) return@forEach
            val icon = vehicleIcons[actor.type] ?: return
            if (actor.type == Plane)
                draw(icon, x, y, dir, planeScale, false)
            else {
                val zoom = !(actor as Vehicle).driverPlayerState.isValid()
                val scale = vehicleScale
                draw(icon, x, y, dir, scale, zoom)
            }
        }
    }

    private fun SpriteBatch.drawGrenades(grenades: ArrayList<renderInfo>?) {
        grenades?.forEach { (actor, x, y, dir) ->
            if (!clipBound.contains(x, y)) return@forEach
            val icon = grenadeIcons[actor.type] ?: return@forEach
            draw(icon, x, y, dir, grenadeScale, true)
        }
    }


    private fun ShapeRenderer.drawAttackLine() {
        val currentTime = System.currentTimeMillis()
        run {
            while (attacks.isNotEmpty()) {
                val (A, B) = attacks.poll()
                attackLineStartTime.add(Triple(A, B, currentTime))
            }
            if (attackLineStartTime.isEmpty()) return@run
            val iter = attackLineStartTime.iterator()
            while (iter.hasNext()) {
                val (A, B, st) = iter.next()
                if (A == selfStateID || B == selfStateID) {
                    if (A != B) {
                        val otherGUID = playerStateToActor[if (A == selfStateID) B else A]
                        if (otherGUID == null) {
                            iter.remove()
                            continue
                        }
                        val other = actors[otherGUID]
                        if (other == null || currentTime - st > attackMeLineDuration) {
                            iter.remove()
                            continue
                        }
                        color = attackLineColor
                        val (xA, yA) = other.location
                        val (xB, yB) = selfCoords
                        line(xA, yA, xB, yB)
                    }
                } else {
                    val actorAID = playerStateToActor[A]
                    val actorBID = playerStateToActor[B]
                    if (actorAID == null || actorBID == null) {
                        iter.remove()
                        continue
                    }
                    val actorA = actors[actorAID]
                    val actorB = actors[actorBID]
                    if (actorA == null || actorB == null || currentTime - st > attackLineDuration) {
                        iter.remove()
                        continue
                    }
                    color = attackLineColor
                    val (xA, yA) = actorA.location
                    val (xB, yB) = actorB.location
                    line(xA, yA, xB, yB)
                }
            }
        }
        run {
            while (firing.isNotEmpty()) {
                val (A, st) = firing.poll()
                actors[A]?.apply {
                    firingStartTime.add(tuple4(location.x, location.y, rotation.y, st))
                }
            }
            if (firingStartTime.isEmpty()) return@run
            val iter = firingStartTime.iterator()
            while (iter.hasNext()) {
                val (x, y, yaw, st) = iter.next()
                if (currentTime - st > firingLineDuration) {
                    iter.remove()
                    continue
                }
                color = firingLineColor
                val (xB, yB) = dirUnitVector.cpy().rotate(yaw).scl(firingLineLength).add(x, y)
                line(x, y, xB, yB)
            }
        }
    }

    private fun ShapeRenderer.drawCircles() {
        Gdx.gl.glLineWidth(2f)
        //vision circle
        color = safeZoneColor
        circle(PoisonGasWarningPosition, PoisonGasWarningRadius, 100)

        color = BLUE
        circle(SafetyZonePosition, SafetyZoneRadius, 100)

        if (PoisonGasWarningPosition.len() > 0) {
            color = safeDirectionColor
            line(selfCoords, PoisonGasWarningPosition)
        }
        Gdx.gl.glLineWidth(1f)


    }

    private fun ShapeRenderer.drawAirDropLine(){
            airDropLocation.values.forEach {
                val (x, y) = it
                val airdropcoords = (Vector2(x, y))
                color = YELLOW
                line(selfCoords, airdropcoords)
            }
    }

    private fun SpriteBatch.drawCorpse() {
        corpseLocation.values.forEach {
            val (x, y) = it
            if (!clipBound.contains(x, y)) return@forEach
            draw(corpseIcon, x, y, 0f, corpseScale, true)
        }
    }

    private fun SpriteBatch.drawAirDrop() {
        airDropLocation.values.forEach {
            val (x, y) = it
            if (!clipBound.contains(x, y)) return@forEach
            draw(carePackage, x, y, -90f, airDropScale, false)
        }
    }

    private fun SpriteBatch.drawRedZoneBomb() {
        val currentTime = System.currentTimeMillis()
        val iter = redZoneBombLocation.entries.iterator()
        while (iter.hasNext()) {
            val (loc, time) = iter.next().value
            val (x, y) = loc
            if (currentTime - time > redzongBombShowDuration)
                iter.remove()
            else if (clipBound.contains(x, y))
                draw(redzoneBombIcon, x, y, 0f, redzoneBombScale, true)
        }
    }

    private fun SpriteBatch.drawItem() {
        // This makes the array empty if the filter is off for performance with an inverted function since arrays are expensive
        scopesToFilter = if (filterScope != 1) {
            arrayListOf("")
        } else {
            arrayListOf(
                    "Item_Attach_Weapon_Upper_Holosight_C",
                    "Item_Attach_Weapon_Upper_DotSight_01_C",
                    "Item_Attach_Weapon_Upper_Aimpoint_C",
                    "Item_Attach_Weapon_Upper_CQBSS_C",
                    "Item_Attach_Weapon_Upper_ACOG_01_C")
        }


        attachToFilter = if (filterAttach != 1) {
            arrayListOf("")
        } else {
            arrayListOf(
                    "Item_Attach_Weapon_Magazine_ExtendedQuickDraw_SniperRifle_C",
                    "Item_Attach_Weapon_Magazine_Extended_SniperRifle_C",
                    "Item_Attach_Weapon_Magazine_ExtendedQuickDraw_Large_C",
                    "Item_Attach_Weapon_Magazine_Extended_Large_C",
                    "Item_Attach_Weapon_Stock_SniperRifle_CheekPad_C",
                    "Item_Attach_Weapon_Stock_SniperRifle_BulletLoops_C",
                    "Item_Attach_Weapon_Stock_AR_Composite_C",
                    "Item_Attach_Weapon_Muzzle_Suppressor_SniperRifle_C",
                    "Item_Attach_Weapon_Muzzle_Suppressor_Large_C",
                    "Item_Attach_Weapon_Muzzle_Suppressor_Medium_C",
                    "Item_Attach_Weapon_Muzzle_FlashHider_Medium_C",
                    "Item_Attach_Weapon_Magazine_ExtendedQuickDraw_Medium_C",
                    "Item_Attach_Weapon_Magazine_Extended_Medium_C",
                    "Item_Attach_Weapon_Muzzle_FlashHider_Large_C",
                    "Item_Attach_Weapon_Muzzle_Compensator_Medium_C",
                    "Item_Attach_Weapon_Lower_Foregrip_C",
                    "Item_Attach_Weapon_Lower_AngledForeGrip_C")
        }

        weaponsToFilter = if (filterWeapon != 1) {
            arrayListOf("")
        } else {
            arrayListOf(
                    "Item_Weapon_AWM_C",
                    "Item_Weapon_M24_C",
                    "Item_Weapon_Kar98k_C",
                    "Item_Weapon_AUG_C",
                    "Item_Weapon_M249_C",
                    "Item_Weapon_Mk14_C",
                    "Item_Weapon_Groza_C",
                    "Item_Weapon_HK416_C",
                    "Item_Weapon_SCAR-L_C",
                    "Item_Weapon_Mini14_C",
                    "Item_Weapon_M16A4_C",
                    "Item_Weapon_SKS_C",
                    "Item_Weapon_AK47_C",
                    "Item_Weapon_DP28_C",
                    "Item_Weapon_Saiga12_C",
                    "Item_Weapon_UMP_C",
                    "Item_Weapon_Vector_C",
                    "Item_Weapon_UZI_C",
                    "Item_Weapon_VSS_C",
                    "Item_Weapon_Thompson_C",
                    "Item_Weapon_Berreta686_C",
                    "Item_Weapon_Winchester_C",
                    "Item_Weapon_Win94_C",
                    "Item_Weapon_G18_C",
                    "Item_Weapon_SawenOff_C",
                    "Item_Weapon_Rhino_C",
                    "Item_Weapon_M1911_C",
                    "Item_Weapon_NagantM1895_C",
                    "Item_Weapon_M9_C")
        }

        healsToFilter = if (filterHeals != 1) {
            arrayListOf("")
        } else {
            arrayListOf(
                    "Item_Heal_Bandage_C",
                    "Item_Heal_MedKit_C",
                    "Item_Heal_FirstAid_C",
                    "Item_Boost_PainKiller_C",
                    "Item_Boost_EnergyDrink_C",
                    "Item_Boost_AdrenalineSyringe_C")
        }

        ammoToFilter = if (filterAmmo != 1) {
            arrayListOf("")
        } else {
            arrayListOf(
                    "Item_Ammo_762mm_C",
                    "Item_Ammo_556mm_C",
                    "Item_Ammo_300Magnum_C",
                    "Item_Weapon_Pan_C",
                    "Item_Ammo_9mm_C",
                    "Item_Ammo_45ACP_C",
                    "Item_Ammo_12Guage_C")
        }

        throwToFilter = if (filterThrow != 1) {
            arrayListOf("")
        } else {
            arrayListOf(
                    "Item_Weapon_Grenade_C",
                    "Item_Weapon_FlashBang_C",
                    "Item_Weapon_SmokeBomb_C",
                    "Item_Weapon_Molotov_C")
        }

        level2Filter = if (filterLvl2 != 1) {
            arrayListOf("")
        } else {
            arrayListOf(
                    "Item_Armor_D_01_Lv2_C",
                    "Item_Armor_C_01_Lv3_C",
                    "Item_Head_G_01_Lv3_C",
                    "Item_Head_F_02_Lv2_C",
                    "Item_Head_F_01_Lv2_C",
                    "Item_Back_C_02_Lv3_C",
                    "Item_Back_C_01_Lv3_C",
                    "Item_Back_F_01_Lv2_C",
                    "Item_Back_F_02_Lv2_C",
                    "Item_Back_E_01_Lv1_C",
                    "Item_Armor_E_01_Lv1_C",
                    "Item_Head_E_01_Lv1_C",
                    "Item_Back_E_02_Lv1_C",
                    "Item_Head_E_02_Lv1_C")
        }

        val Crateitems = arrayListOf("Item_Weapon_AUG",
                "Item_Weapon_M24",
                "Item_Weapon_M249",
                "Item_Weapon_Groza",
                "Item_Weapon_AWM")

        val sorted = ArrayList(droppedItemLocation.values)
        sorted.sortBy {
            order[it._2]
        }
        sorted.forEach {
            if (it._3 && mapCamera.zoom > itemZoomThreshold) return@forEach
            val (x, y) = it._1
            val items = it._2
            val icon = itemIcons[items]
            icon!!
            val scale = if (it._3) itemScale else staticItemScale
            if (items in Crateitems) {
                hpgreen.draw(spriteBatch, "$items", x - scale, y - scale)

                draw(icon, x, y, 0f, scale, it._3)
            }
            if ((items !in weaponsToFilter && items !in scopesToFilter && items !in attachToFilter && items !in level2Filter
                            && items !in ammoToFilter && items !in healsToFilter) && items !in throwToFilter) {

                draw(icon, x, y, 0f, scale, it._3)
            }
        }
    }

    fun drawPlayerInfos(players: MutableList<renderInfo>?) {
        players?.forEach {
            val (actor, x, y, _) = it
            if (!clipBound.contains(x, y)) return@forEach
            val dir = Vector2(x - selfCoords.x, y - selfCoords.y)
            val distance = (dir.len() / 100).toInt()
            val angle = ((dir.angle() + 90) % 360).toInt()
            val (sx, sy) = mapToWindow(x, y)
            val playerStateGUID = (actor as? Character)?.playerStateID ?: return@forEach
            val playerState = actors[playerStateGUID] as? PlayerState ?: return@forEach
            val name = playerState.name
            val health = actorHealth[actor.netGUID] ?: 100f
            val teamNumber = playerState.teamNumber
            val numKills = playerState.numKills
            val equippedWeapons = actorHasWeapons[actor.netGUID]
            val df = DecimalFormat("###.#")
            var weapon = ""
            if (equippedWeapons != null) {
                for (w in equippedWeapons) {
                    val weap = weapons[w ?: continue] as? Weapon ?: continue
                    val result = weap.typeName.split("_")
                    weapon += "${result[2].substring(4)}-->${weap.currentAmmoInClip}\n"
                }
            }
            var items = ""
            for (element in playerState.equipableItems) {
                if (element == null || element._1.isBlank()) continue
                items += "${element._1}->${element._2.toInt()}\n"
            }
            for (element in playerState.castableItems) {
                if (element == null || element._1.isBlank()) continue
                items += "${element._1}->${element._2}\n"
            }


            when (nameToggles) {
                0 ->
                {}

                1 -> {
                    nameFont.draw(spriteBatch,
                            "$angle°${distance}m\n" +
                                    "|N: $name\n" +
                                    "|H: \n" +
                                    "|K: ($numKills)\nTN.($teamNumber)\n" +
                                    "|S: \n" +
                                    "|W: $weapon" +
                                    "|I: $items"

                            , sx + 20, windowHeight - sy + 20)

                    val healthText = health
                    when {
                        healthText > 80f -> hpgreen.draw(spriteBatch, "\n${df.format(health)}", sx + 40, windowHeight - sy + 9)
                        healthText > 33f -> hporange.draw(spriteBatch, "\n${df.format(health)}", sx + 40, windowHeight - sy + 9)
                        else -> hpred.draw(spriteBatch, "\n${df.format(health)}", sx + 40, windowHeight - sy + 9)
                    }

                    if (actor is Character)
                        when {
                            actor.isGroggying -> {
                                hpred.draw(spriteBatch, "DOWNED", sx + 40, windowHeight - sy + -42)
                            }
                            actor.isReviving -> {
                                hporange.draw(spriteBatch, "GETTING REVIVED", sx + 40, windowHeight - sy + -42)
                            }
                            else -> hpgreen.draw(spriteBatch, "Alive", sx + 40, windowHeight - sy + -42)
                        }

                }
                2 -> {
                    nameFont.draw(spriteBatch, "${distance}m\n" +
                            "|N: $name\n" +
                            "|H: ${df.format(health)}\n" +
                            "|W: $weapon",
                            sx + 20, windowHeight - sy + 20)
                }
                3 -> {
                    nameFont.draw(spriteBatch, "|N: $name\n|D: ${distance}m", sx + 20, windowHeight - sy + 20)
                    // rectLine(x - width / 2, hpY, x - width / 2 + healthWidth, hpY, height)
                }
                4 -> {

                    // Change color of hp
                    val healthText = health
                    when {
                        healthText > 80f -> hpgreen.draw(spriteBatch, "\n${df.format(health)}", sx + 40, windowHeight - sy + 8)
                        healthText > 33f -> hporange.draw(spriteBatch, "\n${df.format(health)}", sx + 40, windowHeight - sy + 8)
                        else -> hpred.draw(spriteBatch, "\n${df.format(health)}", sx + 40, windowHeight - sy + 8)

                    }
                    nameFont.draw(spriteBatch, "|N: $name\n|D: ${distance}m $angle°\n" +
                            "|H:\n" +
                            "|S:\n" +
                            "|W: $weapon",
                            sx + 20, windowHeight - sy + 20)

                    if (actor is Character)
                        when {
                            actor.isGroggying -> {
                                hpred.draw(spriteBatch, "DOWNED", sx + 40, windowHeight - sy + -16)
                            }
                            actor.isReviving -> {
                                hporange.draw(spriteBatch, "GETTING REVIVED", sx + 40, windowHeight - sy + -16)
                            }
                            else -> hpgreen.draw(spriteBatch, "Alive", sx + 40, windowHeight - sy + -16)
                        }
                }
            }
        }
    }

    var lastPlayTime = System.currentTimeMillis()
    fun safeZoneHint() {
        if (PoisonGasWarningPosition.len() > 0) {
            val dir = PoisonGasWarningPosition.cpy().sub(selfCoords)
            val road = dir.len() - PoisonGasWarningRadius
            if (road > 0) {
                val runningTime = (road / runSpeed).toInt()
                val (x, y) = dir.nor().scl(road).add(selfCoords).mapToWindow()
                littleFont.draw(spriteBatch, "$runningTime", x, windowHeight - y)
                val remainingTime = (TotalWarningDuration - ElapsedWarningDuration).toInt()
                if (remainingTime == 60 && runningTime > remainingTime) {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastPlayTime > 10000) {
                        lastPlayTime = currentTime
                        alarmSound.play()
                    }
                }
            }
        }
    }

    fun SpriteBatch.draw(texture: TextureRegion, x: Float, y: Float, yaw: Float, scale: Float, zoom: Boolean = true) {
        val w = texture.regionWidth.toFloat()
        val h = texture.regionHeight.toFloat()
        val scale = if (zoom) scale else scale * camera.zoom
        draw(texture, x - w / 2,
                y - h / 2,
                w / 2, h / 2,
                w, h,
                scale, scale,
                yaw)
    }

    inline fun draw(type: ShapeType, draw: ShapeRenderer.() -> Unit) {
        shapeRenderer.apply {
            begin(type)
            draw()
            end()
        }
    }

    inline fun paint(matrix: Matrix4? = null, paint: SpriteBatch.() -> Unit) {
        spriteBatch.apply {
            if (matrix != null) projectionMatrix = matrix
            begin()
            paint()
            end()
        }
    }

    fun ShapeRenderer.circle(loc: Vector2, radius: Float, segments: Int) {
        circle(loc.x, loc.y, radius, segments)
    }

    fun ShapeRenderer.aimAtMe(it: renderInfo) {
        val currentTime = System.currentTimeMillis()
        val (selfX, selfY) = selfCoords
        val zoom = camera.zoom
        //draw aim line
        val (actor, x, y, dir) = it
        if (isTeamMate(actor) >= 0) return
        val actorID = actor.netGUID
        val dirVec = dirUnitVector.cpy().rotate(dir)
        val focus = Vector2(selfX - x, selfY - y)
        val distance = focus.len()
        var aim = false
        if (distance < aimLineRange && distance > aimCircleRadius) {
            val aimAngle = focus.angle(dirVec)
            if (aimAngle.absoluteValue < asin(aimCircleRadius / distance) * MathUtils.radiansToDegrees) {//aim
                aim = true
                aimStartTime.compute(actorID) { _, startTime ->
                    if (startTime == null) currentTime
                    else {
                        if (currentTime - startTime > aimTimeThreshold) {
                            color = aimLineColor
                            rectLine(x, y, selfX, selfY, aimLineWidth * zoom)
                        }
                        startTime
                    }
                }
            }
        }
        if (!aim)
            aimStartTime.remove(actorID)
    }

    fun ShapeRenderer.drawAllPlayerHealth(pColor: Color?, actorInfo: renderInfo) {
        val (actor, x, y, dir) = actorInfo
        if (!clipBound.contains(x, y)) return
        val zoom = camera.zoom
        val backgroundRadius = (playerRadius + 2000f) * zoom

//        val attach = actor.attachChildren.firstOrNull()
//        val teamId = isTeamMate(actor)
//        color = when {
//            teamId >= 0 -> teamColor[teamId]
//            attach == null -> pColor
//            attach == selfID -> selfColor
//            else -> {
//                val teamId = isTeamMate(actors[attach])
//                if (teamId >= 0)
//                    teamColor[teamId]
//                else
//                    pColor
//            }
//        }
//        if (actor is Character)
//            color = when {
//                actor.isGroggying -> {
//                    GRAY
//                }
//                actor.isReviving -> {
//                    WHITE
//                }
//                else -> color
//            }

        if (actor is Character) {//draw health
            val health = if (actor.health <= 0f) actor.groggyHealth else actor.health
            val width = healthBarWidth * zoom
            val height = healthBarHeight * zoom
            val y = y + backgroundRadius + height / 2
            val healthWidth = (health / 100.0 * width).toFloat()
            color = when {
                health > 80f -> GREEN
                health > 33f -> ORANGE
                else -> RED
            }
            rectLine(x - width / 2, y, x - width / 2 + healthWidth, y, height)
        }
    }

    private fun isTeamMate(actor: Actor?): Int {
        val teamID = (actor as? Character)?.teamID ?: return -1
        val team = actors[teamID] as? Team ?: return -1
        return team.memberNumber
    }

    override fun resize(width: Int, height: Int) {
        windowWidth = width.toFloat()
        windowHeight = height.toFloat()
        mapCamera.setToOrtho(true, windowWidth * windowToMapUnit, windowHeight * windowToMapUnit)
        fontCamera.setToOrtho(false, windowWidth, windowHeight)
    }

    override fun pause() {
    }

    override fun resume() {
    }

    override fun dispose() {
        deregister(this)
        alarmSound.dispose()
        largeFont.dispose()
        littleFont.dispose()
        mapErangel.dispose()
        mapMiramar.dispose()
        carePackage.texture.dispose()
        itemAtlas.dispose()
        pawnAtlas.dispose()
        spriteBatch.dispose()
        shapeRenderer.dispose()
        fbo.dispose()
    }

}