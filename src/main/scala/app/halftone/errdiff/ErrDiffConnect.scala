package app.halftone.errdiff

import chisel3._

class PixelGet2ThreshCalc(pixelWidth: Int, errorWidth: Int, posWidth: Int)
  extends Bundle {
  val pos = UInt(posWidth.W)
  val pix = UInt(pixelWidth.W)
  val err = UInt(errorWidth.W)
}

class ThreshCalc2ErrorOut(posWidth: Int, errorWidth: Int)
  extends Bundle {
  val pos = UInt(posWidth.W)
  val bval = Bool()
  val errOut = Vec(4, SInt(errorWidth.W))
}

class ErrorOut2WriteBinary(posWidth: Int) extends Bundle {
  val pos = UInt(posWidth.W)
  val bval = Bool()
}