package tools

import chisel3._
import chisel3.util.{Enum, is, switch}
import utils.FPGAModule

/**
 * The default direction is from Master to Slave
 * Unless otherwise specified
 */

class SimpleDataPortReq(val awidth: Int,
                        val dwidth: Int,
                        val isW: Boolean) extends Bundle {
  val addr = Output(UInt(awidth.W))
  val valid = Output(Bool())
  if(isW) {
    val data = Output(UInt(dwidth.W))
  }
}

class SimpleDataPortResp(val dwidth: Int) extends Bundle {
  val data = Input(UInt(dwidth.W))
  val valid = Input(Bool())
}

class SimpleDataPort(val awidth: Int, val dwidth: Int, isW: Boolean = false) extends Bundle {
  val req = new SimpleDataPortReq(awidth, dwidth, isW)
  val resp = new SimpleDataPortResp(dwidth)
}

class AXI4MasterModule(val awidth: Int,
                       val dwidth: Int,
                       val lite: Boolean = true) extends FPGAModule {
  val io = FlatIO(new Bundle {
    val axi = new AXI4Lite
    val read = Flipped(new SimpleDataPort(awidth, dwidth))
//    val write = Flipped(new SimpleDataPort(awidth, dwidth, isW = true))
  })

  // Stay all ports
  dontTouch(io)
  io.axi <> DontCare

  // FSM
  val sIdle :: sWaitReady :: sWaitData :: Nil = Enum(3)
  val state = RegInit(sIdle)

  switch(state) {
    is(sIdle) {
      when(io.read.req.valid) { state := sWaitReady }
    }
    is(sWaitReady) {
      when(io.axi.ar.fire) { state := sWaitData }
    }
    is(sWaitData) {
      when(io.axi.r.fire) { state := sIdle }
    }
  }

  // emit address signal
  io.axi.ar.valid := state === sWaitReady
  io.axi.ar.bits.addr := Mux(state === sWaitReady, io.read.req.addr, 0.U)
  // receive data
  io.axi.r.ready := state === sWaitData
  io.read.resp.valid := io.axi.r.fire
  io.read.resp.data := io.axi.r.bits.data
}
