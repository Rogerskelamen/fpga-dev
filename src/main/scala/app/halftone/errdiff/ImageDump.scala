package app.halftone.errdiff

import app.halftone.ErrDiffConfig
import chisel3._
import chisel3.util.{Counter, Decoupled}
import tools.bus.{BramNativePortFull, SimpleDataPortR, SimpleDataPortW}

class ImageDumpRead(config: ErrDiffConfig) extends Module {
  val io = IO(new Bundle {
    val in = Flipped(Decoupled())
    val read  = new SimpleDataPortR(awidth = config.ddrWidth, dwidth = config.ddrWidth)
    val pa = Flipped(new BramNativePortFull(config.bramDataBits, config.bramAddrBits))
    val out = Decoupled()
  })

  // Registers
  val readDone = RegInit(false.B)
  val busy = RegInit(false.B)
  val resultValid = RegInit(false.B)
  val (cnt, cnt_wrap) = Counter(io.read.resp.valid && !resultValid, config.imageSiz)

  io.out.valid := resultValid
  io.in.ready  := !busy
  // useless signals
  io.out.bits := DontCare

  val offset = cnt(1, 0)
  // activate to work and every time one reading finishes
  readDone := io.read.resp.valid || io.in.fire
  // read image data(one pixel) from ddr
  io.read.req.valid := readDone && !resultValid
  io.read.req.addr := cnt + config.ddrBaseAddr.U

  // write one pixel data to bram
  io.pa.en := io.read.resp.valid
  io.pa.we := io.read.resp.valid
  io.pa.addr := cnt
  io.pa.din := io.read.resp.data >> (offset << 3.U)

  when(busy) {
    when(cnt_wrap && io.read.resp.valid) {
      resultValid := true.B
    }
    when(io.out.fire) {
      busy        := false.B
      resultValid := false.B
    }
  }.otherwise {
    when(io.in.valid) {
      busy := true.B
    }
  }
}

class ImageDumpWrite(config: ErrDiffConfig) extends Module {
  val io = IO(new Bundle {
    val in = Flipped(Decoupled())
    val write = new SimpleDataPortW(awidth = config.ddrWidth, dwidth = config.ddrWidth)
    val pb = Flipped(new BramNativePortFull(config.bramDataBits, config.bramAddrBits))
    val out = Decoupled()
  })

  val writeDone = RegInit(false.B)
  val delay_r = RegInit(false.B)
  val busy = RegInit(false.B)
  val resultValid = RegInit(false.B)
  val (cnt, cnt_wrap) = Counter(io.write.resp.valid && !resultValid, config.imageSiz)

  io.out.valid := resultValid
  io.in.ready  := !busy
  // useless signals
  io.pb.we := false.B
  io.pb.din := 0.U
  io.out.bits := DontCare

  val offset = cnt(1, 0)
  delay_r := io.pb.en
  writeDone := io.write.resp.valid || io.in.fire
  // read binary(one pixel) from bram
  io.pb.en := writeDone && !resultValid
  io.pb.addr := cnt

  // write binary data to ddr
  io.write.req.valid := delay_r
  io.write.req.addr := cnt + config.ddrBaseAddr.U
  io.write.req.strb := 1.U << offset
  io.write.req.data  := io.pb.dout << (offset << 3.U)

  when(busy) {
    when(cnt_wrap && io.write.resp.valid) {
      resultValid := true.B
    }
    when(io.out.fire) {
      busy        := false.B
      resultValid := false.B
    }
  }.otherwise {
    when(io.in.valid) {
      busy := true.B
    }
  }
}
