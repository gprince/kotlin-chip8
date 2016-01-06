package chip8fx.chip

import chip8fx.chip.extensions.eachByte
import chip8fx.chip.extensions.toIntAndMask
import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream
import java.io.OutputStream
import java.util.*

private val BYTE_MASK = 0xFF
private val MASK_0x00FF = 0x00FF
private val MASK_0x0FFF = 0x0FFF
private val MASK_0xFFFF = 0xFFFF
private val MASK_0xF000 = 0xF000
private val MASK_0x0F00 = 0x0F00
private val MASK_0x00F0 = 0x00F0
private val MASK_0x000F = 0x000F
private val MASK_0xF00F = 0xF00F
private val MASK_0xF0FF = 0xF0FF

private inline fun <reified T> createAndfill(size: Int, elem: () -> T): Array<T> = Array(size) { elem() }

interface OpcodeProcessor {

    val processor: Chip.(Int) -> Unit

    fun accept(opcode: Int): Boolean = false
    fun process(chip: Chip, opcode: Int): Unit = chip.processor(opcode)
}

public enum class InstructionEnum {
    UNKNOWN, ADD, AND, CALL, CLS, DRW, JP, LD, OR, RET, RND, SE, SHL, SHR, SKNP, SKP, SNE, SUB, SUBN, SYS, XOR;
}

public enum class OpcodeProcessorStrategyEnum private constructor(
        val instruction: InstructionEnum,
        val acceptedOpcode: Int,
        override val processor: Chip.(Int) -> Unit = {},
        val acceptMask: Int = MASK_0xF000) : OpcodeProcessor {

    UNKNOWN(InstructionEnum.UNKNOWN, 0xFFFF, { opcode -> println(opcode) },
            MASK_0xFFFF),

    CLS(InstructionEnum.CLS, 0x00E0,
            {
                clearScreen()
                programCounter += 2
                needRedraw = true
            },
            MASK_0xFFFF),

    RET(InstructionEnum.RET, 0x00EE,
            {
                --stackPointer
                programCounter = stack[stackPointer] + 2
            },
            MASK_0xFFFF),

    SYS_Addr(InstructionEnum.SYS, 0x0000),

    JP_Addr(InstructionEnum.JP, 0x1000,
            {
                opcode ->
                programCounter = opcode and MASK_0x0FFF
            }),

    CALL_Addr(InstructionEnum.CALL, 0x2000,
            {
                opcode ->
                stack[stackPointer] = programCounter
                ++stackPointer
                programCounter = opcode and MASK_0x0FFF
            }),

    SE_Vx_Byte(InstructionEnum.SE, 0x3000,
            {
                opcode ->
                val x = (opcode and MASK_0x0F00) shr 8
                val nn = opcode and MASK_0x00FF
                programCounter += if (registers[x] == nn) {
                    4
                } else {
                    2
                }
            }),

    SNE_Vx_Byte(InstructionEnum.SNE, 0x4000,
            {
                opcode ->
                val x = (opcode and MASK_0x0F00) shr 8
                val nn = opcode and MASK_0x00FF
                programCounter += if (registers[x] != nn) {
                    4
                } else {
                    2
                }
            }),

    SE_Vx_Vy(InstructionEnum.SE, 0x5000,
            {
                opcode ->
                val x = (opcode and MASK_0x0F00) shr 8
                val y = (opcode and MASK_0x00F0) shr 4
                programCounter += if (registers[x] == registers[y]) {
                    4
                } else {
                    2
                }
            }),

    LD_Vx_Byte(InstructionEnum.LD, 0x6000,
            {
                opcode ->
                val x = (opcode and MASK_0x0F00) shr 8
                registers[x] = opcode and MASK_0x00FF
                programCounter += 2
            }),

    ADD_Vx_Byte(InstructionEnum.ADD, 0x7000,
            {
                opcode ->
                val x = (opcode and MASK_0x0F00) shr 8
                val nn = opcode and MASK_0x00FF
                registers[x] = (registers[x] + nn) and BYTE_MASK
                programCounter += 2

            }),

    LD_Vx_Vy(InstructionEnum.LD, 0x8000,
            {
                opcode ->
                val x = (opcode and MASK_0x0F00) shr 8
                val y = (opcode and MASK_0x00F0) shr 4
                registers[x] = registers[y]
                programCounter += 2
            },
            MASK_0xF00F),

    OR_Vx_Vy(InstructionEnum.OR, 0x8001,
            {
                opcode ->
                val x = (opcode and MASK_0x0F00) shr 8
                val y = (opcode and MASK_0x00F0) shr 4
                registers[x] = (registers[x] or registers[y]) and BYTE_MASK
                programCounter += 2
            },
            MASK_0xF00F),

    AND_Vx_Vy(InstructionEnum.AND, 0x8002,
            {
                opcode ->
                val x = (opcode and MASK_0x0F00) shr 8
                val y = (opcode and MASK_0x00F0) shr 4
                registers[x] = (registers[x] and registers[y]) and BYTE_MASK
                programCounter += 2
            },
            MASK_0xF00F),

    XOR_Vx_Vy(InstructionEnum.XOR, 0x8003,
            {
                opcode ->
                val x = (opcode and MASK_0x0F00) shr 8
                val y = (opcode and MASK_0x00F0) shr 4
                registers[x] = (registers[x] xor registers[y]) and BYTE_MASK
                programCounter += 2
            },
            MASK_0xF00F),

    ADD_Vx_Vy(InstructionEnum.ADD, 0x8004,
            {
                opcode ->
                val x = (opcode and MASK_0x0F00) shr 8
                val y = (opcode and MASK_0x00F0) shr 4
                registers[0xF] = if (registers[y] > 0xFF - registers[x]) {
                    1
                } else {
                    0
                }
                registers[x] = (registers[x] + registers[y]) and BYTE_MASK
                programCounter += 2
            },
            MASK_0xF00F),

    SUB_Vx_Vy(InstructionEnum.SUB, 0x8005,
            {
                opcode ->
                val x = (opcode and MASK_0x0F00) shr 8
                val y = (opcode and MASK_0x00F0) shr 4
                registers[0xF] = if (registers[x] > registers[y]) {
                    1
                } else {
                    0
                }
                registers[x] = (registers[x] - registers[y]) and BYTE_MASK
                programCounter += 2
            },
            MASK_0xF00F),

    SHR_Vx_Vy(InstructionEnum.SHR, 0x8006,
            {
                opcode ->
                val x = (opcode and MASK_0x0F00) shr 8
                registers[0xF] = registers[x] and 0x1
                registers[x] = registers[x] shl 1
                programCounter += 2
            },
            MASK_0xF00F),
    SUBN_Vx_Vy(InstructionEnum.SUBN, 0x8007,
            {
                opcode ->
                val x = (opcode and MASK_0x0F00) shr 8
                val y = (opcode and MASK_0x00F0) shr 4
                registers[0xF] = if (registers[x] > registers[y]) {
                    0
                } else {
                    1
                }
                registers[x] = (registers[x] - registers[y]) and BYTE_MASK
                programCounter += 2
            },
            MASK_0xF00F),

    SHL_Vx_Vy(InstructionEnum.SHL, 0x800E,
            {
                opcode ->
                val x = (opcode and MASK_0x0F00) shr 8
                registers[0xF] = registers[x] and 0x80
                registers[x] = registers[x] shl 1
                programCounter += 2
            },
            MASK_0xF00F),

    SNE_Vx_Vy(InstructionEnum.SNE, 0x9000,
            {
                opcode ->
                val x = (opcode and MASK_0x0F00) shr 8
                val y = (opcode and MASK_0x00F0) shr 4
                programCounter += if (registers[x] != registers[y]) {
                    4
                } else {
                    2
                }
            }),

    LD_I_Addr(InstructionEnum.LD, 0xA000,
            {
                opcode ->
                memoryPointer = opcode and MASK_0x0FFF
                programCounter += 2
            }),

    JP_V0_Addr(InstructionEnum.JP, 0xB000,
            {
                opcode ->
                val addr = opcode and MASK_0x0FFF
                val extra = registers[0] and BYTE_MASK
                programCounter = addr + extra
            }),

    RND_Vx_Byte(InstructionEnum.RND, 0xC000,
            {
                opcode ->
                val x = (opcode and MASK_0x0F00) shr 8
                val nn = opcode and MASK_0x00FF
                registers[x] = Random().nextInt(255) and nn
                programCounter += 2
            }),

    DRW_Vx_Vy_Nibble(InstructionEnum.DRW, 0xD000,
            {
                opcode ->
                val x = (opcode and MASK_0x0F00) shr 8
                val y = (opcode and MASK_0x00F0) shr 4
                val vx = registers[x]
                val vy = registers[y]
                val height = opcode and MASK_0x000F
                registers[0xF] = 0

                for (_y in 0..height - 1) {
                    val line = memory[memoryPointer + _y]
                    for (_x in 0..7) {
                        val pixel = line and (0x80 shr _x)
                        if (pixel != 0) {
                            val totalX = (vx + _x) % 64
                            val totalY = (vy + _y) % 32
                            val index = totalY * 64 + totalX

                            if (display[index] == 1) {
                                registers[0xF] = 1
                            }

                            display[index] = display[index] xor 1
                        }
                    }
                }

                programCounter += 2
                needRedraw = true
            }),

    SKP_Vx(InstructionEnum.SKP, 0xE09E,
            {
                opcode ->
                val x = (opcode and MASK_0x0F00) shr 8
                val key = registers[x]
                programCounter += if (keys[key] == 1) {
                    4
                } else {
                    2
                }
            },
            MASK_0xF0FF),

    SKNP_Vx(InstructionEnum.SKNP, 0xE0A1,
            {
                opcode ->
                val x = (opcode and MASK_0x0F00) shr 8
                val key = registers[x]
                programCounter += if (keys[key] == 0) {
                    4
                } else {
                    2
                }
            },
            MASK_0xF0FF),

    LD_Vx_DT(InstructionEnum.LD, 0xF007,
            {
                opcode ->
                val x = (opcode and MASK_0x0F00) shr 8
                registers[x] = delayTimer
                programCounter += 2
            },
            MASK_0xF0FF),

    LD_Vx_K(InstructionEnum.LD, 0xF00A,
            {
                opcode ->
                val x = (opcode and MASK_0x0F00) shr 8
                for (index in 0..keys.size - 1) {
                    if (keys[index] == 1) {
                        registers[x] = index
                        programCounter += 2
                        break
                    }
                }
            },
            MASK_0xF0FF),

    LD_DT_Vx(InstructionEnum.LD, 0xF015,
            {
                opcode ->
                val x = (opcode and MASK_0x0F00) shr 8
                delayTimer = registers[x]
                programCounter += 2
            },
            MASK_0xF0FF),

    LD_ST_Vx(InstructionEnum.LD, 0xF018,
            {
                opcode ->
                val x = (opcode and MASK_0x0F00) shr 8
                soundTimer = registers[x]
                programCounter += 2
            },
            MASK_0xF0FF),

    ADD_I_Vx(InstructionEnum.ADD, 0xF01E,
            {
                opcode ->
                val x = (opcode and MASK_0x0F00) shr 8
                memoryPointer += registers[x]
                programCounter += 2
            },
            MASK_0xF0FF),

    LD_F_Vx(InstructionEnum.LD, 0xF029,
            {
                opcode ->
                val x = (opcode and MASK_0x0F00) shr 8
                val character = registers[x]
                memoryPointer = 0x050 + character * 5
                programCounter += 2
            },
            MASK_0xF0FF),

    LD_B_Vx(InstructionEnum.LD, 0xF033,
            {
                opcode ->
                val x = (opcode and MASK_0x0F00) shr 8
                var value = registers[x]
                val hundreds = (value - (value % 100)) / 100
                value -= hundreds * 100
                val tens = (value - (value % 10)) / 10
                value -= tens * 10
                memory[memoryPointer] = hundreds
                memory[memoryPointer + 1] = tens
                memory[memoryPointer + 2] = value
                programCounter += 2
            },
            MASK_0xF0FF),

    LD_I_Vx(InstructionEnum.LD, 0xF055,
            {
                opcode ->
                val x = opcode and MASK_0x0F00 shr 8
                for (index in 0..x) {
                    memory[memoryPointer + index] = registers[index]
                }
                programCounter += 2
            },
            MASK_0xF0FF),

    LD_Vx_I(InstructionEnum.LD, 0xF065,
            {
                opcode ->
                val x = opcode and MASK_0x0F00 shr 8
                for (index in 0..x) {
                    registers[index] = memory[memoryPointer + index]
                }
                memoryPointer += x + 1
                programCounter += 2
            },
            MASK_0xF0FF);

    companion object {

        fun decode(opcode: Int): OpcodeProcessorStrategyEnum {
            OpcodeProcessorStrategyEnum.values().forEach {
                instruction ->
                if (instruction.accept(opcode)) {
                    return instruction
                }
            }
            return UNKNOWN
        }
    }

    override fun accept(opcode: Int): Boolean = this.acceptedOpcode.equals(opcode and acceptMask)
}

class Chip {

    /**
     * # 4k of 8-bit memory
     */

    val memory: Array<Int>

    /**
     * # 16 x 8-bit registers
     */
    val registers: Array<Int>

    /**
     * # 16-bit memory pointer
     */
    var memoryPointer: Int

    /**
     * # 16-bit operation pointer
     */
    var programCounter: Int

    var address: Int

    val stack: Array<Int>

    var stackPointer: Int

    var delayTimer: Int

    var soundTimer: Int

    var keys: Array<Int>

    val display: Array<Int>

    var needRedraw: Boolean

    init {
        memory = createAndfill(4096) { 0x00 }
        registers = createAndfill(4096) { 0x00 }
        memoryPointer = 0x00
        programCounter = 0x200
        address = 0x200

        stack = createAndfill(16) { 0x00 }
        stackPointer = 0

        delayTimer = 0
        soundTimer = 0

        keys = createAndfill(16) { 0x00 }
        display = createAndfill(64 * 32) { 0x00 }

        needRedraw = false

        loadFontset()
    }

    public fun clearScreen() {
        display.fill(0x00)
    }

    public fun run() {

        // fetch Opcode
        val opcode = (memory[programCounter] shl 8 or memory[programCounter + 1])
        OpcodeProcessorStrategyEnum.decode(opcode).process(this, opcode)

        if(soundTimer > 0) {
            soundTimer--
            // Play sound
        }

        if(delayTimer > 0) {
            delayTimer--
        }
    }

    private fun loadFontset() {
        ChipData.fontset.forEachIndexed {
            index, font ->
            memory[0x50 + index] = font and BYTE_MASK
        }
    }

    public fun loadProgram(fileName: String) {

        println("Loading $fileName...")

        DataInputStream(FileInputStream(File(fileName))).use {
            stream ->
            address = 0x1FF
            stream.eachByte {
                byte ->
                memory[++address] = byte toIntAndMask BYTE_MASK
            }
        }

        println("Program loaded!")
    }

    public fun setKeyBuffer(keyBuffer: Array<Int>) {
        keyBuffer.forEachIndexed { index, key -> keys[index] = key }
    }

    public fun displayDump(stream: OutputStream) {

            val bufferedWriter = stream.bufferedWriter()

            display.forEachIndexed {
                index, value ->
                if (index % 64 == 0) {
                    println()
                }

                if (value == 0) {
                    print("\u25A1")
                } else {
                    print("\u25A0")
                }

            }

            stream.flush()

    }

    public fun memoryDump(stream: OutputStream) {
        stream.use {
            stream ->
            var col = 1
            val bufferedWriter = stream.bufferedWriter()
            memory.forEach {
                bufferedWriter.write("%02X ".format(it))
                ++col
                if (col > 16) {
                    col = 1
                    bufferedWriter.newLine()
                    bufferedWriter.flush()
                }
            }
            stream.flush()
        }
    }
}