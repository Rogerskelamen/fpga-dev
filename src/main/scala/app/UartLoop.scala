package app

import chisel3._
import devices.Uart.UART_BITW
import devices.{UartConfig, UartRecv, UartTran}
import utils.{EdgeDetector, FPGAModule}

class UartLoop extends FPGAModule {
  // use customized clock and rest
  override protected def clkFPGAName: String = "sys_clk"
  override protected def rstFPGAName: String = "sys_rst_n"

  // IO ports
  val io = FlatIO(new Bundle {
    val recv_done = Input(Bool())
    val recv_data = Input(UInt(UART_BITW.W))
    val tx_busy = Input(Bool())
    val trans_en = Output(Bool())
    val trans_data = Output(UInt(UART_BITW.W))
  })

  // Detect raising of recv_done
  val recv_done_flag = EdgeDetector(io.recv_done)
  val tx_ready = RegInit(false.B)

  val trans_en_r = RegInit(false.B)
  val trans_data_r = RegInit(0.U(UART_BITW.W))
  io.trans_en := trans_en_r
  io.trans_data := trans_data_r

  when(recv_done_flag) {
    tx_ready := true.B
    trans_en_r := false.B
    trans_data_r := io.recv_data
  }.elsewhen(!io.tx_busy && tx_ready) {
    tx_ready := false.B
    trans_en_r := true.B
    trans_data_r := io.recv_data
  }
}

class UartLoopTop extends FPGAModule {
  val io = FlatIO(new Bundle {
    val uart_rxd = Input(Bool())
    val uart_txd = Output(Bool())
  })

  // Submodule
  val uartRecv = Module(new UartRecv())
  val uartTran = Module(new UartTran())
  val uartLoop = Module(new UartLoop)

  uartRecv.fpga_rst := fpga_rst
  uartRecv.fpga_clk := fpga_clk
  uartTran.fpga_rst := fpga_rst
  uartTran.fpga_clk := fpga_clk
  uartLoop.fpga_rst := fpga_rst
  uartLoop.fpga_clk := fpga_clk

  uartRecv.io.uart_rxd := io.uart_rxd

  uartLoop.io.recv_data := uartRecv.io.uart_data
  uartLoop.io.recv_done := uartRecv.io.uart_done
  uartLoop.io.tx_busy := uartTran.io.uart_tx_busy

  uartTran.io.uart_en := uartLoop.io.trans_en
  uartTran.io.uart_din := uartLoop.io.trans_data
  io.uart_txd := uartTran.io.uart_txd
}
