package chip8fx.chip

import org.junit.Test
import kotlin.test.fail

class OpcodeProcessorStrategyEnumTest {

    // Opcodes list
    val opcodes = arrayOf(
            0x00E0, // CLS
            0x00EE, // RET
            0x0000, // 0x0nnn - SYS addr
            0x1000, // 0x1nnn - JP addr
            0x2000, // 0x2nnn - CALL addr
            0x3000, // 0x3xkk - SE Vx, byte
            0x4000, // 0x4xkk - SNE Vx, byte
            0x5000, // 0x5xy0 - SE Vx, Vy
            0x6000, // 0x6xkk - LD Vx, byte
            0x7000, // 0x7xkk - ADD Vx, byte
            0x8000, // 0x8xy0 - LD Vx, Vy
            0x8001, // 0x8xy1 - OR Vx, Vy
            0x8002, // 0x8xy2 - AND Vx, Vy
            0x8003, // 0x8xy3 - XOR Vx, Vy
            0x8004, // 0x8xy4 - ADD Vx, Vy
            0x8005, // 0x8xy5 - SUB Vx, Vy
            0x8006, // 0x8xy6 - SHR Vx { , Vy }
            0x8007, // 0x8xy7 - SUBN Vx, Vy
            0x800E, // 0x8xy8 - SHL Vx { , Vy }
            0x9000, // 0x9xy0 - SNE Vx, Vy
            0xA000, // 0xAnnn - LD I, addr
            0xB000, // 0xBnnn - JP V0, addr
            0xC000, // 0xCxkk - RND Vx, byte
            0xD000, // 0x8xyn - DRW Vx, Vy, nibble
            0xE09E, // 0xEx9E - SKP Vx
            0xE0A1, // 0xExA1 - SKNP Vx
            0xF007, // 0xFx07 - LD Vx, DT
            0xF00A, // 0xFx0A - LD Vx, K
            0xF015, // 0xFx15 - LD DT, Vx
            0xF018, // 0xFx18 - LD ST, Vx
            0xF01E, // 0xFx1E - ADD I, Vx
            0xF029, // 0xFx29 - LD F, Vx
            0xF033, // 0xFx33 - LD B, Vx
            0xF055, // 0xFx55 - LD [I], Vx
            0xF065  // 0xFx65 - LD Vx, [I]
    )

    @Test
    fun decode_should_not_return_UNKNOWN() {

        // Given a list of opcodes

        // #docode(Int) should not return UNKNOWN
        opcodes.forEach {
            opcode ->
            if (OpcodeProcessorStrategyEnum.UNKNOWN.equals(OpcodeProcessorStrategyEnum.decode(opcode))) {
                fail("Decoding opcode: ${"%04x".format(opcode)} return UNKNOWN!")
            }
        }
    }


}