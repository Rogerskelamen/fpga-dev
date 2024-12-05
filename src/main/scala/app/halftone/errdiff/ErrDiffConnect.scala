package app.halftone.errdiff

import chisel3._

class PixelGet2ThreshCalc(pixelWidth: Int, errorWidth: Int, posWidth: Int)
  extends Bundle {
  val pos = UInt(posWidth.W)
  val pix = UInt(pixelWidth.W)
  val err = UInt(errorWidth.W)
}
