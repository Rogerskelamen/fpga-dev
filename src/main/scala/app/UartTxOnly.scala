package app

import chisel3._
import devices.{UartConfig, UartTran}
import utils.FPGAModule

object UartTxOnly {
  val tran_data = "h68".U
}

class UartTxOnly extends FPGAModule {
  val io = FlatIO(new Bundle {
    val txd = Output(Bool())
  })

  import UartTxOnly._
  val uartTran = Module(new UartTran())
  val data_en = RegInit(false.B)

  // connections
  uartTran.fpga_clk := fpga_clk
  uartTran.fpga_rst := fpga_rst
  uartTran.io.uart_din := tran_data
  uartTran.io.uart_en := data_en
  io.txd := uartTran.io.uart_txd

  val cnt = RegInit(0.U(3.W))
  when(!uartTran.io.uart_tx_busy) {
    cnt := cnt + 1.U
    when(cnt === 7.U) { // delay eight cycles to activate
      data_en := true.B
    }
  }.otherwise {
    data_en := false.B
    cnt := 0.U
  }
}
