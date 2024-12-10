package app.halftone.errdiff

import chisel3._
import chisel3.experimental.VecLiterals.AddObjectLiteralConstructor
import chisel3.simulator.EphemeralSimulator._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

object ELUTTest {
  val errorWidth: Int = 8
}

class ELUTTest extends AnyFreeSpec with Matchers {
  "ELUT should emit correct error value" in {
    import ELUTTest._
    simulate(new ELUT(errorWidth)) { dut =>
      dut.io.err poke -127.S // simple signed number
      dut.io.out(0) expect -56.S(errorWidth.W)
      dut.io.out(1) expect -8.S(errorWidth.W)
      dut.io.out(2) expect -40.S(errorWidth.W)
      dut.io.out(3) expect -24.S(errorWidth.W)

      dut.io.err poke -100.S
      dut.io.out(0) expect -44.S(errorWidth.W)
      dut.io.out(1) expect -6.S(errorWidth.W)
      dut.io.out(2) expect -31.S(errorWidth.W)
      dut.io.out(3) expect -19.S(errorWidth.W)
    }
  }
}
