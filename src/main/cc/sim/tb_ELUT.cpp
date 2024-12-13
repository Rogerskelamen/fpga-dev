#include <verilated.h>
#include <verilated_vcd_c.h>
#include "VELUT.h"

VerilatedContext *contextp = NULL;
VerilatedVcdC *m_trace = NULL;

#define MAX_SIM_TIME 10

int main() {
  contextp = new VerilatedContext;
  m_trace = new VerilatedVcdC;
  VELUT *top = new VELUT;
  Verilated::traceEverOn(true);
  top->trace(m_trace, 0);
  m_trace->open("waveform.vcd");

  top->io_err = 0x9c; // 0b10011100 => -100
  /*
   * outputs should be
   * -44 => 0xd4
   * -6  => 0xfa
   * -31 => 0xe1
   * -19 => 0xed
   */
  for(int i = 0; i < MAX_SIM_TIME; i++) {
    top->eval();
    contextp->timeInc(1);
    m_trace->dump(contextp->time());
  }

  m_trace->close();
  delete top;
}
