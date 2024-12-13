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
      for (err <- -127 to -127) {
        // input data, calculate by hardware
        dut.io.err poke err.S(errorWidth.W)

        // calculate result manually
        val errF = err.toFloat
        val result0 = scala.math.round(errF * 7/16)
        val result1 = scala.math.round(errF * 1/16)
        val result2 = scala.math.round(errF * 5/16)
        val result3 = scala.math.round(errF * 3/16)

        // compare hardware result with hand calc
        dut.io.out(0) expect result0.S
        dut.io.out(1) expect result1.S
        dut.io.out(2) expect result2.S
        dut.io.out(3) expect result3.S
      }
    }
  }
}
