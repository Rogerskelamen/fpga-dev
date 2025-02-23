package tools

import chisel3._
import chisel3.util.Cat
import utils.FPGAModule

/**
 * A module emits random 1/0 single bit signal
 * @param dwidth
 */
class RandomEmitter(val dwidth: Int) extends Module {
  require(dwidth >= 8, "data width needs to be greater than 8")

  val io = FlatIO(new Bundle {
    val out = Output(Bool())
  })

  val lfsr = RegInit(3.U(dwidth.W))
  val xor = lfsr(0) ^ lfsr(2) ^ lfsr(3) ^ lfsr(5)
  lfsr := Cat(xor, lfsr(dwidth-1, 1))

  io.out := lfsr(0)
}
