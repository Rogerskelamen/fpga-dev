package tools.bus

import chisel3._

object BramNativePortParam {
  val dataWidth = 32
  val addrWidth = 12
}

/** It's in the view of BRAM, So
  * Direction: from Slave to Master
  */
trait HasWriteEnable {
  def dwidth: Int = BramNativePortParam.dataWidth
  val din = Input(UInt(dwidth.W))
  val we  = Input(Bool())
}

class BramNativePort(val dwidth: Int = BramNativePortParam.dataWidth,
                     val awidth: Int = BramNativePortParam.addrWidth)
    extends Bundle {
  val en   = Input(Bool())
  val dout = Output(UInt(dwidth.W))
  val addr = Input(UInt(awidth.W))
}

class BramNativePortFull(
  override val dwidth: Int = BramNativePortParam.dataWidth,
  override val awidth: Int = BramNativePortParam.addrWidth)
    extends BramNativePort
    with HasWriteEnable

/** Bram Port A: serve for writing
  * Bram Port B: Serve for reading
  */
class BramNativeDualPorts(
  val dwidth: Int = BramNativePortParam.dataWidth,
  val awidth: Int = BramNativePortParam.addrWidth)
    extends Bundle {
  val pb = Flipped(new BramNativePortFull(dwidth, awidth))
  val pa = Flipped(new BramNativePortFull(dwidth, awidth))
}
