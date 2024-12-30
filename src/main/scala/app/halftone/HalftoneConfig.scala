package app.halftone

import chisel3.util.unsignedBitLength

abstract class HalftoneConfig(
  val pixelWidth:  Int,
  val ddrBaseAddr: Int,
  val ddrWidth:    Int,
  val imageRow:    Int,
  val imageCol:    Int) {
  def imageSiz: Int = imageRow * imageCol
  def posWidth: Int = unsignedBitLength(imageSiz - 1)
}
