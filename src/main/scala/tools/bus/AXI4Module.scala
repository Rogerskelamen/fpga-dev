package tools.bus

import chisel3._
import chisel3.util.{Enum, is, switch}
import utils.FPGAModule

class AXI4MasterModule(val awidth: Int,
                       val dwidth: Int,
                       val lite: Boolean = true) extends FPGAModule {
  // data/address width of AXI4 bus is fixed for now
  // data width = address width = 32 bits
  val io = FlatIO(new Bundle {
    val axi = new AXI4Lite
    val read = Flipped(new SimpleDataPortR(awidth, dwidth))
    val write = Flipped(new SimpleDataPortW(awidth, dwidth))
  })

  // FSM
  val sIdle :: sWaitReady :: sWaitData :: sWaitResp :: Nil = Enum(4)
  val rstate = RegInit(sIdle)
  val wstate = RegInit(sIdle)

  /*
   * FSM for Read transaction
   */
  switch(rstate) {
    is(sIdle) {
      when(io.read.req.valid) { rstate := sWaitReady }
    }
    is(sWaitReady) {
      when(io.axi.ar.fire) { rstate := sWaitData }
    }
    is(sWaitData) {
      when(io.axi.r.fire) { rstate := sIdle }
    }
  }

  // emit address signal
  io.axi.ar.valid := rstate === sWaitReady
  io.axi.ar.bits.addr := Mux(rstate === sWaitReady, io.read.req.addr, 0.U)
  // receive data
  io.axi.r.ready := rstate === sWaitData
  io.read.resp.valid := io.axi.r.fire
  io.read.resp.data := io.axi.r.bits.data

  /*
   * FSM for Write transaction
   */
  val aw_fire_r = RegInit(false.B)
  val w_fire_r = RegInit(false.B)
  when(io.axi.aw.fire) { aw_fire_r := true.B }
  when(io.axi.w.fire) { w_fire_r := true.B }
  when(wstate === sWaitResp) {
    aw_fire_r := false.B
    w_fire_r := false.B
  }

  switch(wstate) {
    is(sIdle) {
      when(io.write.req.valid) { wstate := sWaitReady }
    }
    is(sWaitReady) {
      when(aw_fire_r && w_fire_r) { wstate := sWaitResp }
    }
    is(sWaitResp) {
      when(io.axi.b.fire) { wstate := sIdle }
    }
  }

  // emit address and data/wmask signals
  io.axi.aw.valid := wstate === sWaitReady
  io.axi.w.valid := wstate === sWaitReady
  io.axi.aw.bits.addr := Mux(wstate === sWaitReady, io.write.req.addr, 0.U)
  io.axi.w.bits.data := Mux(wstate === sWaitReady, io.write.req.data, 0.U)
  io.axi.w.bits.strb := "b1111".U
  // get response
  io.axi.b.ready := wstate === sWaitResp
  io.write.resp.valid := io.axi.b.fire
  io.write.resp.data := Mux(io.axi.b.fire, 0.U, ~0.U)
}
