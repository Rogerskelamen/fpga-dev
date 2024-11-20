package utils

import chisel3._
import chisel3.simulator.EphemeralSimulator._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class DownCounterTest extends AnyFreeSpec with Matchers {
  "DownCounter should wrap at proper clock time" in {
    simulate(new DownCounterGen(8)) { dut =>
      dut.reset poke true.B
      dut.clock.step(2)

      dut.reset poke false.B
      dut.clock.step(7)
      dut.io.wrap expect true.B
    }
  }
}
