package utils

import chisel3._
import chisel3.simulator.EphemeralSimulator._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class DownCounterWrap(cond: Bool, n: Int) extends Module {
  val io = IO(new Bundle {
    val wrap = Output(Bool())
  })
  val wrap = DownCounter(cond, n)
  io.wrap := wrap
}

class DownCounterTest extends AnyFreeSpec with Matchers {
  "DownCounter should wrap at proper condition" in {
    simulate(new DownCounterWrap(true.B, 20)) { dut =>
      dut.clock.step(20)
      dut.io.wrap(true.B)
    }
  }
}
