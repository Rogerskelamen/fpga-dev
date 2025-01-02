#include <verilated.h>
#include <verilated_vcd_c.h>
#include "VErrDiffCore.h"
#include "VErrDiffCore___024root.h"
#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include <assert.h>
#include <math.h>

#define IMG_H 16
#define IMG_W 16
#define THRES 128

// global variables
static int errCache[IMG_H][IMG_W] = {0};
static uint8_t img[IMG_H][IMG_W];

VerilatedContext *contextp = NULL;
VerilatedVcdC *m_trace = NULL;
VErrDiffCore *top = NULL;

static uint32_t
random_gen(uint32_t max) {
  srand(rand());
  return rand() % max;
}

static void
valid_then_acc(int i, int j, int val) {
  if (0 <= i && i < IMG_H && 0 <= j && j < IMG_W) {
    errCache[i][j] += val;
  }
}

void init_sim() {
  contextp = new VerilatedContext;
  m_trace = new VerilatedVcdC;
  top = new VErrDiffCore;
  Verilated::traceEverOn(true);
  top->trace(m_trace, 0);
  m_trace->open("waveform.vcd");
}

void step_and_dump_wave() {
  top->clk ^= 1;
  top->eval();
  contextp->timeInc(1);
  m_trace->dump(contextp->time());

  top->clk ^= 1;
  top->eval();
  contextp->timeInc(1);
  m_trace->dump(contextp->time());
}

void exit_sim() {
  m_trace->close();
  delete top;
}

int main() {
  init_sim();

  unsigned int seed = 42;
  srand(seed);

  // generate gray-image using random generator
  for (int pos = 0; pos < IMG_H*IMG_W; pos++)
    img[pos/IMG_H][pos%IMG_W] = random_gen(0x100);

  // init reset
  top->rstn = 0; top->extn_ready = 1;
  for (int i = 0; i < 5; i++)
    step_and_dump_wave();

  // trigger hardware
  top->rstn = 1; top->extn_ready = 0; step_and_dump_wave(); step_and_dump_wave();
  top->extn_ready = 1; step_and_dump_wave();

  for (int i = 0; i < 30; i++)
    step_and_dump_wave();

  for (int i = 0; i < IMG_H; i++) {
    for (int j = 0; j < IMG_W; j++) {
      uint8_t pix = img[i][j];
      int err = 0;
      while(top->rootp->ErrDiffCore__DOT__threshCalc__DOT__resultValid) step_and_dump_wave();
    }
  }

  exit_sim();
}
