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

  val rfsr = RegInit(3.U(dwidth.W))
  val xor = rfsr(0) ^ rfsr(2) ^ rfsr(3) ^ rfsr(5)
  rfsr := Cat(xor, rfsr(dwidth-1, 1))

  io.out := rfsr(0)
}
