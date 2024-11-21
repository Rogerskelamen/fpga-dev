TOPNAME = DownCounterGen

# Directories
BUILD_DIR = ./build
VERILOG_GEN = ./verilog-gen
VSIM_DIR = ./src/main/cc/sim

TARGET = $(abspath $(VERILOG_GEN)/$(TOPNAME).v)
BIN = $(abspath $(BUILD_DIR)/$(TOPNAME))
SRC = $(abspath $(VSIM_DIR)/tb_$(TOPNAME).cpp)

# Waveform
WAVEFILE = waveform.vcd
WAVECONFIG = .gtkwave.config

# Verilog simulator
VERILATOR = verilator
VERILATOR_CFLAGS = -MMD --cc --build --trace

verilog:
	sbt run

wave: sim
	@gtkwave $(WAVEFILE) -r $(WAVECONFIG)

sim: $(BIN)
	@$(BIN)

$(BIN): $(TARGET) $(SRC)
	@mkdir -p $(BUILD_DIR)
	$(VERILATOR) $(VERILATOR_CFLAGS) \
		--top-module $(TOPNAME) $^ \
		--exe --Mdir $(BUILD_DIR)/obj_dir -o $(abspath $(BIN))

clean:
	-rm -rf $(BUILD_DIR)
