#include <verilated.h>
#include <verilated_vcd_c.h>
#include "VDownCounterGen.h"

VerilatedContext *contextp = NULL;
VerilatedVcdC *m_trace = NULL;

#define MAX_SIM_TIME 40

int main() {
  contextp = new VerilatedContext;
  m_trace = new VerilatedVcdC;
  VDownCounterGen *top = new VDownCounterGen;
  Verilated::traceEverOn(true);
  top->trace(m_trace, 0);
  m_trace->open("waveform.vcd");

  top->clock ^= 1; top->reset = 0;
  top->eval();
  contextp->timeInc(1);
  m_trace->dump(contextp->time());
  top->clock ^= 1; top->reset = 0;
  top->eval();
  contextp->timeInc(1);
  m_trace->dump(contextp->time());
  top->clock ^= 1; top->reset = 1;
  top->eval();
  contextp->timeInc(1);
  m_trace->dump(contextp->time());
  top->clock ^= 1; top->reset = 1;
  top->eval();
  contextp->timeInc(1);
  m_trace->dump(contextp->time());

  for(int i = 0; i < MAX_SIM_TIME; i++) {
    top->reset = 0;
    top->clock ^= 1;
    top->eval();
    contextp->timeInc(1);
    m_trace->dump(contextp->time());
  }

  m_trace->close();
  delete top;
}
