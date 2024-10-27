package component

import chisel3._
import component.Uart._
import utils.{EdgeDetector, FPGAModule}

object Uart {
  val CLK_FREQ = 50000000
  val UART_BAUD = 115200
  final def BPS_CNT = CLK_FREQ/UART_BAUD
}

class Uart {

}

class UartRec extends FPGAModule(true) {
  override protected def clkFPGAName: String = "sys_clk"
  override protected def rstFPGAName: String = "sys_rst_n"

  val io = FlatIO(new Bundle {
    val uart_rxd = Input(Bool())
  })

  // Detect falling of uart_rxd signal
  val start_flag = EdgeDetector(io.uart_rxd, isRaise = false)

  val rx_flag = RegInit(false.B)
  val rx_cnt = RegInit(0.U(4.W))
  val clk_cnt = RegInit(0.U(9.W))
  // when to receive data
  when(start_flag) { rx_flag := true.B }
  .elsewhen(rx_cnt === 9.U && clk_cnt === (BPS_CNT/2).U) {
    rx_flag := false.B
  }

  when(rx_flag) {
    clk_cnt := Mux(clk_cnt < (BPS_CNT-1).U, clk_cnt + 1.U, 0.U)
  }.otherwise {
    clk_cnt := 0.U
  }
}
