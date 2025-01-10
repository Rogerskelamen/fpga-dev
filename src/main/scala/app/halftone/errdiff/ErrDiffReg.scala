package app.halftone.errdiff

import app.halftone.errdiff.ErrDiffReg.{ActiveAddr, DoneAddr, EndAddr, StartAddr}
import chisel3._
import tools.bus.{SimpleDataPortR, SimpleDataPortW}

object ErrDiffReg {
  val ActiveAddr = "h4000_0000".U
  val DoneAddr   = "h4000_0004".U
  val StartAddr  = "h4000_0008".U
  val EndAddr    = "h4000_000c".U
}

class ErrDiffReg(ddrWidth: Int) extends Module {
  val io = IO(new Bundle {
    val read  = Flipped(new SimpleDataPortR(ddrWidth, ddrWidth))
    val write = Flipped(new SimpleDataPortW(ddrWidth, ddrWidth))
    val reg = new Bundle {
      val active = Output(Bool())
      val done   = Input(Bool())
      val start  = Input(Bool())
      val end    = Input(Bool())
    }
  })

  /*
   * Registers
   */
  // control registers
  val active_r = RegInit(false.B)
  val done_r   = RegInit(false.B)
  // timer registers
  val start_r = RegInit(false.B)
  val end_r   = RegInit(false.B)

  // normal registers
  val read_data_r = Reg(Bool())

  // IO Connections
  io.reg.active := active_r
  done_r        := io.reg.done
  start_r       := io.reg.start
  end_r         := io.reg.end

  /*
   * Logics
   */
  // delay registers for response valid signal
  val write_req_valid_r = RegNext(io.write.req.valid, false.B)
  io.write.resp.valid := write_req_valid_r
  io.write.resp.data  := 0.U

  val read_req_valid_r = RegNext(io.read.req.valid, false.B)
  io.read.resp.valid := read_req_valid_r
  io.read.resp.data  := read_data_r

  // write
  when(io.write.req.valid) {
    assert(io.write.req.addr === ActiveAddr, "Only support writing to active register[0x4000_0000]")
    active_r := io.write.req.data
  }

  // offset: off
  // read
  when(io.read.req.valid) {
    when(io.read.req.addr === DoneAddr) { read_data_r := done_r }
      .elsewhen(io.read.req.addr === StartAddr) { read_data_r := start_r }
      .elsewhen(io.read.req.addr === EndAddr) { read_data_r := end_r }
      .otherwise {
        assert(false.B, cf"Only support reading from register done[$DoneAddr], start[$StartAddr], end[$StartAddr]")
        read_data_r := false.B
      }
  }
  // offset: on
}
