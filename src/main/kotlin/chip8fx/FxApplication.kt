package chip8fx

import chip8fx.chip.Chip
import javafx.application.Application
import javafx.concurrent.Task
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.canvas.Canvas
import javafx.scene.control.Button
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCode.*
import javafx.scene.input.KeyEvent
import javafx.scene.layout.AnchorPane
import javafx.scene.paint.Color
import javafx.stage.Stage


class FxApplication : Application() {

    inner class Chip8Task : Task<Nothing>() {
        override fun call(): Nothing? {

            println("Started!")

            try {
                while (!Thread.interrupted()) {

                    chip.run()
                    if (chip.needRedraw) {
                        repaint()
                        chip.needRedraw = false
                    }

                    Thread.sleep(4)
                }
            } catch (e: InterruptedException) {
                println("Program interrupted!")
                System.exit(0)
            }

            return null
        }
    }

    val chip: Chip
    val task: Chip8Task

    init {
        chip = Chip()
        chip.loadProgram(Chip::class.java.getResource("../roms/ufo.rom").file)
        task = Chip8Task()
    }

    private lateinit var display: Canvas

    override fun start(primaryStage: Stage) {
        primaryStage.isResizable = false
        primaryStage.title = "Chip8 FX"

        val loader = FXMLLoader()
        primaryStage.scene = with (loader) {
            location = FxApplication::class.java.getResource("views/Display.fxml")
            val root: AnchorPane = load()
            Scene(root, 640.0, 400.0)
        }

        val btnStart = primaryStage.scene.lookup("#btnStart") as Button
        btnStart.setOnAction { event -> Thread(task).start() }

        this.display = primaryStage.scene.lookup("#display") as Canvas

        primaryStage.setOnCloseRequest { task.cancel() }
        primaryStage.addEventHandler(KeyEvent.KEY_PRESSED) {
            event ->
            val keyIndex = keyIndexFromKeyCode(event.code)
            if (keyIndex != -1)
                chip.keys[keyIndex] = 1
        }
        primaryStage.addEventHandler(KeyEvent.KEY_RELEASED) {
            event ->
            val keyIndex = keyIndexFromKeyCode(event.code)
            if (keyIndex != -1)
                chip.keys[keyIndex] = 0
        }
        primaryStage.show()
    }

    private fun keyIndexFromKeyCode(code: KeyCode): Int =
            when (code) {
                DIGIT1 -> 1
                DIGIT2 -> 2
                DIGIT3 -> 3
                A      -> 4
                Z      -> 5
                E      -> 6
                Q      -> 7
                S      -> 8
                D      -> 9
                W      -> 0xA
                X      -> 0
                C      -> 0xB
                DIGIT4 -> 0xC
                R      -> 0xD
                F      -> 0xE
                V      -> 0xF
                else   -> -1
            }

    private fun repaint() {

        val gc = this.display.graphicsContext2D

        chip.display.forEachIndexed {
            index, value ->
            gc.fill = if (value == 0) {
                Color.BLACK
            } else {
                Color.AQUA
            }

            val px = index % 64
            val py = Math.floor(index / 64.0)

            gc.fillRect(px * 10.0, py * 10.0, 10.0, 10.0)
        }
    }

}

fun main(args: Array<String>) {
    Application.launch(FxApplication::class.java, *args)
}