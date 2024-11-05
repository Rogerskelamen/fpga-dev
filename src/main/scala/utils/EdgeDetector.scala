package utils

import chisel3._

object EdgeDetector {
  def apply(detected: Bool, isRaise: Boolean = true): Bool = {
    val gen = Wire(Bool())
    val sync = RegInit(VecInit(Seq.fill(2)(false.B)))
    sync(0) := detected
    sync(1) := sync(0)
    if (isRaise) { gen := sync(0) & !sync(1) }
    else { gen := !sync(0) & sync(1) }
    gen
  }
}

