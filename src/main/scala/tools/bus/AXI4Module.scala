package tools.bus

import chisel3._
import chisel3.util.{Enum, is, switch}
import utils.FPGAModule

class AXI4MasterModule(val awidth: Int,
                       val dwidth: Int,
                       val lite: Boolean = true) extends FPGAModule {
  /**
   * data/address width of AXI4 bus is fixed for now
   * data width = address width = 32 bits
   */
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
  io.axi.ar.bits.addr := io.read.req.addr
  // receive data
  io.axi.r.ready := rstate === sWaitData
  io.read.resp.valid := io.axi.r.fire
  io.read.resp.data := io.axi.r.bits.data

  /*
   * FSM for Write transaction
   */
  /**
   * Write Transaction is different from Read because
   * You don't know who comes first(AW or W?)
   * Anyway, there are two rules you should always obey:
   * 1. valid should be de-asserted immediately when handshake finishes
   * 2. B depends on AW/W (W weekly depends on AW)
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
      when((aw_fire_r && io.axi.w.fire) ||
           (w_fire_r && io.axi.aw.fire)
      ) { wstate := sWaitResp }
    }
    is(sWaitResp) {
      when(io.axi.b.fire) { wstate := sIdle }
    }
  }

  // AW Channel
  val aw_valid_r = RegInit(false.B)
  when(wstate === sIdle && io.write.req.valid && !aw_valid_r) { aw_valid_r := true.B }
  .elsewhen(io.axi.aw.fire) { aw_valid_r := false.B }
  io.axi.aw.valid := aw_valid_r
  // emit address
  io.axi.aw.bits.addr := io.write.req.addr

  // W Channel
  val w_valid_r = RegInit(false.B)
  when(wstate === sIdle && io.write.req.valid && !w_valid_r) { w_valid_r := true.B }
  .elsewhen(io.axi.w.fire) { w_valid_r := false.B }
  io.axi.w.valid := w_valid_r
  // emit data/wmask signals
  io.axi.w.bits.data := io.write.req.data
  io.axi.w.bits.strb := "b1111".U

  // B Channel -- get response
  io.axi.b.ready := wstate === sWaitResp
  io.write.resp.valid := io.axi.b.fire
  io.write.resp.data := Mux(io.axi.b.fire, 0.U, ~0.U)
}

class AXI4SlaveModule(val awidth: Int,
                      val dwidth: Int,
                      val lite: Boolean = true) extends FPGAModule {
  val io = FlatIO(new Bundle {
    val axi = Flipped(new AXI4Lite)
  })

  // FSM
  val sIdle :: sWaitValid :: sResp :: Nil = Enum(3)
  // val rstate = RegInit(sIdle)
  val wstate = RegInit(sIdle)

  /*
   * FSM for Read transaction
   */
  io.axi.r <> DontCare
  io.axi.ar <> DontCare

  /*
   * FSM for Write transaction
   */
  // AW first, W second
  switch(wstate) {
    is(sIdle) {
      when(io.axi.aw.fire) { wstate := sWaitValid }
    }
    is(sWaitValid) {
      when(io.axi.w.fire) { wstate := sResp }
    }
    is(sResp) {
      when(io.axi.b.fire) { wstate := sIdle }
    }
  }

  io.axi.aw.ready := wstate === sIdle
  val addr = RegInit(0.U(awidth.W))
  when(io.axi.aw.fire) {
    addr := io.axi.aw.bits.addr
  }
  io.axi.w.ready := wstate === sWaitValid
  val data = RegInit(0.U(dwidth.W))
  when(io.axi.w.fire) {
    data := io.axi.w.bits.data
  }
  io.axi.b.valid := wstate === sResp
  io.axi.b.bits.resp := 0.U
}
