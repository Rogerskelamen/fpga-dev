TOPNAME = DownCounterGen
BUILD_DIR = ./build
VERILOG_GEN = ./verilog-gen
VSIM_DIR = ./src/main/cc/sim
BIN = $(BUILD_DIR)/$(TOPNAME)
WAVEFILE = waveform.vcd
WAVECONFIG = .gtkwave.config
SRC = $(abspath $(VSIM_DIR)/tb_$(TOPNAME).cpp)

VERILATOR = verilator
VERILATOR_CFLAGS = -cc --build --trace

verilog:
	sbt run

wave: sim
	@gtkwave $(WAVEFILE) -r $(WAVECONFIG)

sim: $(BIN)
	@$(BIN)

$(BIN): $(VERILOG_GEN)/$(TOPNAME).v $(SRC)
	@mkdir -p $(BUILD_DIR)
	$(VERILATOR) $(VERILATOR_CFLAGS) \
		--top-module $(TOPNAME) $^ \
		--exe --Mdir $(BUILD_DIR)/obj_dir -o $(abspath $(BIN))

clean:
	-rm -rf $(BUILD_DIR)
