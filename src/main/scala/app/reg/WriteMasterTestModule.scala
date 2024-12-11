package app.reg

import chisel3._
import chisel3.util.{Enum, is, switch}
import tools.bus.AXI4Lite
import utils.FPGAModule

class WriteMasterTestModule extends FPGAModule {
  val io = FlatIO(new Bundle {
    val axi = new AXI4Lite
    val extn_ready = Input(Bool())
  })

  io.axi.ar <> DontCare
  io.axi.r <> DontCare

  val sIdle :: sWaitReady :: sWaitResp :: Nil = Enum(3)
  val state = RegInit(sIdle)
  val triggered = RegInit(false.B)

  switch(state) {
    is(sIdle) {
      when(!io.extn_ready && !triggered) { state := sWaitReady }
    }
    is(sWaitReady) {
      when(io.axi.aw.fire && io.axi.w.fire) { state := sWaitResp }
    }
    is(sWaitResp) {
      when(io.axi.b.fire) { state := sIdle }
    }
  }

  // AW/W Channel
  io.axi.aw.valid := state === sWaitReady
  io.axi.aw.bits.addr := "h4000_0000".U
  io.axi.w.valid := state === sWaitReady
  io.axi.w.bits.strb := "b1111".U
  io.axi.w.bits.data := 1.U

  // B Channel
  io.axi.b.ready := state === sWaitResp

  when(state =/= sIdle) {
    triggered := true.B
  }
}
