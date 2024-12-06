package app.halftone

import chisel3.util.unsignedBitLength

abstract class HalftoneConfig(
  val pixelWidth: Int = 8,
  // ddrBaseAddr: Int = ???,
  val ddrBaseAddr: Int = 0,
  val imageRow:    Int = 512,
  val imageCol:    Int = 512) {
  def imageSiz: Int = imageRow * imageCol
  def posWidth: Int = unsignedBitLength(imageSiz - 1)
}
