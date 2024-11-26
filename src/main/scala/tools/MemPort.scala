package tools

import chisel3._
import chisel3.util.Decoupled

/**
 * This MemPort interface just intends to get data and write data
 * it doesn't function like a real bus
 */
class MemReq(val awidth: Int, val dwidth: Int) extends Bundle {
  val addr = Output(UInt(awidth.W))
  val data = Output(UInt(dwidth.W))
}

class MemResp(val dwidth: Int) extends Bundle {
  val data = Output(UInt(dwidth.W))
}

// Direction: Master to Slave
class MemPort(val awidth: Int, val dwidth: Int) extends Bundle {
  val req  = Decoupled(new MemReq(awidth, dwidth))
  val resp = Flipped(Decoupled(new MemResp(dwidth)))
}
