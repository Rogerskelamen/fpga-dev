package tools.bus

import chisel3._

/** The default direction is from Master to Slave
  * Unless otherwise specified
  */

/** This DataPort interface just intends to get data and write data
  * it doesn't function like a real bus
  */
class SimpleDataPortReqR(val awidth: Int) extends Bundle {
  val addr  = Output(UInt(awidth.W))
  val valid = Output(Bool())
}

class SimpleDataPortReqW(val awidth: Int, val dwidth: Int) extends Bundle {
  val addr  = Output(UInt(awidth.W))
  val data  = Output(UInt(dwidth.W))
  val strb  = Output(UInt((dwidth/8).W))
  val valid = Output(Bool())
}

class SimpleDataPortResp(val dwidth: Int) extends Bundle {
  val data  = Input(UInt(dwidth.W))
  val valid = Input(Bool())
}

class SimpleDataPortR(val awidth: Int, val dwidth: Int) extends Bundle {
  val req  = new SimpleDataPortReqR(awidth)
  val resp = new SimpleDataPortResp(dwidth)
}

class SimpleDataPortW(val awidth: Int, val dwidth: Int) extends Bundle {
  val req  = new SimpleDataPortReqW(awidth, dwidth)
  val resp = new SimpleDataPortResp(dwidth)
}
