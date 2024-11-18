package app.led

import chisel3._
import chisel3.util.{Counter, Enum, unsignedBitLength}
import utils.FPGAModule

object PulseLight {
  val FREQ = 100_000_000
  val HALF_DUR = 1 // unit: s
  def CYCLE_NR: Int = Math.sqrt(FREQ*HALF_DUR).toInt
}

class PulseLight extends FPGAModule {
  val io = FlatIO(new Bundle {
    val out = Output(Bool())
  })

  import PulseLight._
  // use PWM technology
  val (cnt, cnt_wrap) = Counter(true.B, CYCLE_NR)
  val cnt_cycle = RegInit(0.U(unsignedBitLength(CYCLE_NR-1).W))

  // FSM
  val sINC :: sDEC :: Nil = Enum(2)
  val state = RegInit(sINC)

  when(state === sINC) {
    when(cnt_wrap) { cnt_cycle := cnt_cycle + 1.U }
    when(cnt_cycle === (CYCLE_NR-1).U) { state := sDEC }
  }.elsewhen(state === sDEC) {
    when(cnt_wrap) { cnt_cycle := cnt_cycle - 1.U }
    when(cnt_cycle === 0.U) { state := sINC }
  }

  io.out := cnt > cnt_cycle
}
