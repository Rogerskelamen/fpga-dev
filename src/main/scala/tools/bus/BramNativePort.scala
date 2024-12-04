package tools.bus

import chisel3._

object BramNativePortParam {
  val dataWidth = 8
  val addrWidth = 18
}

// It's in the view of BRAM, So
// Direction: from Slave to Master
trait HasWriteEnable {
  val din = Input(UInt(BramNativePortParam.dataWidth.W))
  val we = Input(Bool())
}

class BramNativePort(val dwidth: Int = BramNativePortParam.dataWidth,
                     val awidth: Int = BramNativePortParam.addrWidth)
  extends Bundle {
  val en = Input(Bool())
  val dout = Output(UInt(dwidth.W))
  val addr = Input(UInt(awidth.W))
}

class BramNativePortFull extends BramNativePort with HasWriteEnable